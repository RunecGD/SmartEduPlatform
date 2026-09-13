import json
import re

import httpx
from sqlalchemy import text

from app.config import settings
from app.db import engine


def get_random_chunks(lesson_id: int, limit: int = 10) -> list[str]:
    """Получает случайные фрагменты материалов конкретного урока."""
    with engine.connect() as conn:
        rows = conn.execute(text("""
            SELECT mc.content
            FROM material_chunks mc
            JOIN materials m ON m.id = mc.material_id
            WHERE m.lesson_id = :lesson_id
            ORDER BY random()
            LIMIT :limit
        """), {
            "lesson_id": lesson_id,
            "limit": limit,
        }).fetchall()

    return [row.content for row in rows]


def parse_json_response(response: str):
    """Извлекает JSON, даже если LLM обернула его в ```json ... ```."""
    response = response.strip()

    match = re.search(
        r"```(?:json)?\s*(.*?)\s*```",
        response,
        flags=re.DOTALL | re.IGNORECASE,
    )

    if match:
        response = match.group(1).strip()

    return json.loads(response)


def generate_exam(lesson_id: int, count: int = 5) -> list[dict]:
    """Генерирует вопросы по материалам урока."""

    if not 1 <= count <= 20:
        raise ValueError("Количество вопросов должно быть от 1 до 20")

    chunks = get_random_chunks(lesson_id)

    if not chunks:
        raise ValueError(
            "Для этого урока нет проиндексированных материалов"
        )

    context = "\n\n---\n\n".join(chunks)

    prompt = f"""
Ты — преподаватель образовательной платформы.

Используя ТОЛЬКО приведённые ниже фрагменты учебных материалов,
сгенерируй {count} вопросов с открытым ответом.

Требования:
- вопросы должны проверять понимание материала;
- не задавай вопросы, ответа на которые нет в материалах;
- вопросы должны быть разными;
- каждый вопрос должен иметь максимальный балл 10;
- отвечай на русском языке.

Верни СТРОГО JSON-массив следующего формата:

[
  {{
    "question": "Текст вопроса",
    "max_score": 10
  }}
]

Фрагменты учебных материалов:

{context}
"""

    with httpx.Client(timeout=180) as client:
        response = client.post(
            f"{settings.ollama_url}/api/generate",
            json={
                "model": settings.llm_model,
                "prompt": prompt,
                "stream": False,
                "format": "json",
                "options": {
                    "temperature": 0.2,
                },
            },
        )

        response.raise_for_status()

    raw_response = response.json()["response"]
    questions = parse_json_response(raw_response)

    if not isinstance(questions, list):
        raise ValueError("LLM вернула не JSON-массив вопросов")

    result = []

    for item in questions:
        if not isinstance(item, dict):
            continue

        question = item.get("question")
        max_score = item.get("max_score", 10)

        if not question:
            continue

        result.append({
            "question": str(question),
            "max_score": int(max_score),
        })

    if not result:
        raise ValueError("LLM не вернула корректных вопросов")

    return result


def grade_attempt(answers: list[dict]) -> list[dict]:
    """Проверяет ответы студента с помощью LLM."""

    results = []

    with httpx.Client(timeout=180) as client:

        for item in answers:
            question_id = item["question_id"]
            question = item["question"]
            max_score = int(item["max_score"])
            student_answer = item["answer_text"]
            chunks = item.get("chunks", [])

            context = "\n\n---\n\n".join(chunks)

            prompt = f"""
Ты — проверяющий преподаватель.

Оцени ответ студента на основе вопроса и учебного материала.

Важно:
- используй ТОЛЬКО информацию из учебного материала;
- не требуй от студента формулировки слово в слово;
- учитывай смысл ответа;
- если ответ частично правильный — поставь частичный балл;
- если ответ полностью неправильный — поставь 0;
- максимальный балл: {max_score};
- укажи ошибки или недостающую информацию;
- отвечай на русском языке.

Верни СТРОГО JSON:

{{
  "score": 7,
  "feedback": "Ответ в целом правильный, но..."
}}

Вопрос:
{question}

Учебный материал:
{context}

Ответ студента:
{student_answer}
"""

            response = client.post(
                f"{settings.ollama_url}/api/generate",
                json={
                    "model": settings.llm_model,
                    "prompt": prompt,
                    "stream": False,
                    "format": "json",
                    "options": {
                        "temperature": 0.1,
                    },
                },
            )

            response.raise_for_status()

            raw_response = response.json()["response"]
            data = parse_json_response(raw_response)

            score = int(data.get("score", 0))

            # Защита от некорректного ответа LLM
            score = max(0, min(score, max_score))

            results.append({
                "question_id": question_id,
                "score": score,
                "feedback": str(data.get("feedback", "")),
            })

    return results