import json

from openai import OpenAI
from sqlalchemy import text

from app.config import settings
from app.db import engine


client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=settings.openrouter_api_key,
)


def get_random_chunks(
    lesson_id: int,
    limit: int = 5,
) -> list[str]:

    with engine.connect() as conn:
        rows = conn.execute(
            text("""
                SELECT mc.content
                FROM material_chunks mc
                JOIN materials m
                    ON m.id = mc.material_id
                WHERE m.lesson_id = :lesson_id
                ORDER BY random()
                LIMIT :limit
            """),
            {
                "lesson_id": lesson_id,
                "limit": limit,
            },
        ).fetchall()

    return [row.content for row in rows]


def generate_exam(
    lesson_id: int,
    count: int = 5,
) -> list[dict]:

    if not 1 <= count <= 20:
        raise ValueError(
            "Количество вопросов должно быть от 1 до 20"
        )

    chunks = get_random_chunks(
        lesson_id=lesson_id,
        limit=5,
    )

    if not chunks:
        raise ValueError(
            "Для этого урока нет проиндексированных материалов"
        )

    context = "\n\n---\n\n".join(chunks)

    response = client.chat.completions.create(
        model=settings.openrouter_model,

        messages=[
            {
                "role": "system",
                "content": """
Ты — преподаватель образовательной платформы.

Используй ТОЛЬКО предоставленный учебный материал.

Сгенерируй вопросы с открытым ответом.

Требования:
- вопросы должны проверять понимание материала;
- не используй информацию, которой нет в материале;
- каждый вопрос оценивается максимум в 10 баллов;
- не добавляй ответы на вопросы;
- верни только JSON.
""",
            },
            {
                "role": "user",
                "content": f"""
Сгенерируй {count} вопросов.

Учебный материал:

{context}
""",
            },
        ],

        temperature=0.2,

        response_format={
            "type": "json_schema",
            "json_schema": {
                "name": "exam_questions",
                "strict": True,
                "schema": {
                    "type": "object",
                    "properties": {
                        "questions": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "question": {
                                        "type": "string"
                                    },
                                    "max_score": {
                                        "type": "integer"
                                    },
                                },
                                "required": [
                                    "question",
                                    "max_score",
                                ],
                                "additionalProperties": False,
                            },
                        },
                    },
                    "required": [
                        "questions"
                    ],
                    "additionalProperties": False,
                },
            },
        },
    )

    content = response.choices[0].message.content

    if not content:
        raise ValueError(
            "OpenRouter не вернул ответ"
        )

    data = json.loads(content)

    questions = data["questions"]

    result = []

    for item in questions:

        question = item.get("question")

        if not question:
            continue

        try:
            max_score = int(
                item.get("max_score", 10)
            )
        except (TypeError, ValueError):
            max_score = 10

        max_score = max(
            1,
            min(max_score, 100),
        )

        result.append(
            {
                "question": str(question),
                "max_score": max_score,
            }
        )

    if not result:
        raise ValueError(
            "OpenRouter не вернул корректных вопросов"
        )

    return result


def grade_attempt(
    answers: list[dict],
) -> list[dict]:

    results = []

    for item in answers:

        question_id = item["question_id"]
        question = item["question"]
        max_score = int(item["max_score"])
        student_answer = item["answer_text"]

        chunks = item.get("chunks", [])

        context = "\n\n---\n\n".join(chunks)

        response = client.chat.completions.create(
            model=settings.openrouter_model,

            messages=[
                {
                    "role": "system",
                    "content": """
Ты — проверяющий преподаватель.

Оцени ответ студента ТОЛЬКО на основе
вопроса и предоставленного учебного материала.

Правила:
- учитывай смысл ответа;
- не требуй дословного совпадения;
- полностью правильный ответ получает максимальный балл;
- частично правильный ответ получает частичный балл;
- неправильный ответ получает 0;
- укажи ошибки или недостающую информацию;
- отвечай на русском языке;
- верни только JSON.
""",
                },
                {
                    "role": "user",
                    "content": f"""
Вопрос:
{question}

Максимальный балл:
{max_score}

Учебный материал:
{context}

Ответ студента:
{student_answer}
""",
                },
            ],

            temperature=0.1,

            response_format={
                "type": "json_schema",
                "json_schema": {
                    "name": "grade_result",
                    "strict": True,
                    "schema": {
                        "type": "object",
                        "properties": {
                            "score": {
                                "type": "integer"
                            },
                            "feedback": {
                                "type": "string"
                            },
                        },
                        "required": [
                            "score",
                            "feedback",
                        ],
                        "additionalProperties": False,
                    },
                },
            },
        )

        content = response.choices[0].message.content

        if not content:
            raise ValueError(
                "OpenRouter не вернул результат проверки"
            )

        data = json.loads(content)

        score = int(
            data.get("score", 0)
        )

        score = max(
            0,
            min(score, max_score),
        )

        results.append(
            {
                "question_id": question_id,
                "score": score,
                "feedback": str(
                    data.get("feedback", "")
                ),
            }
        )

    return results