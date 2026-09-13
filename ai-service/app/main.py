import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sqlalchemy import text

from app.config import settings
from app.db import engine
from app.ingest import index_material
from app.rag import ask as rag_ask
from app.examiner import generate_exam, grade_attempt


app = FastAPI(
    title="SmartEdu AI Service",
    version="0.3.0",
)


@app.get("/health")
def health():
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))

    return {
        "status": "ok",
        "db": "connected",
        "llm_model": settings.llm_model,
        "embed_model": settings.embed_model,
    }


class IndexRequest(BaseModel):
    object_key: str


class AskRequest(BaseModel):
    question: str
    lesson_id: int


class GenerateExamRequest(BaseModel):
    lesson_id: int
    count: int = 5


class GradeQuestion(BaseModel):
    id: int
    text: str
    max_score: int
    chunks: list[str] = []


class GradeAnswer(BaseModel):
    question_id: int
    answer_text: str


class GradeAttemptRequest(BaseModel):
    questions: list[GradeQuestion]
    answers: list[GradeAnswer]


@app.post("/index")
def index(req: IndexRequest):
    with engine.connect() as conn:
        row = conn.execute(text("""
            SELECT id, file_name, content_type
            FROM materials
            WHERE object_key = :ok
        """), {
            "ok": req.object_key
        }).fetchone()

    if not row:
        raise HTTPException(404, "Материал не найден")

    try:
        with httpx.Client(timeout=60) as client:
            r = client.get(
                f"{settings.minio_url}/"
                f"{settings.minio_bucket}/"
                f"{req.object_key}"
            )
            r.raise_for_status()
            content = r.content

    except httpx.HTTPError:
        raise HTTPException(
            502,
            "Не удалось скачать файл из хранилища"
        )

    count = index_material(
        row.id,
        content,
        row.content_type,
        row.file_name,
    )

    return {
        "material_id": row.id,
        "chunks_indexed": count,
    }


@app.post("/ask")
def ask(req: AskRequest):
    return rag_ask(
        req.question,
        req.lesson_id,
    )


@app.post("/exams/generate")
def generate_exam_endpoint(req: GenerateExamRequest):
    try:
        return generate_exam(
            lesson_id=req.lesson_id,
            count=req.count,
        )
    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail=str(e),
        )


@app.post("/exams/grade")
def grade_exam_endpoint(req: GradeAttemptRequest):
    questions_by_id = {
        question.id: question
        for question in req.questions
    }

    answers_for_grading = []

    for answer in req.answers:
        question = questions_by_id.get(answer.question_id)

        if question is None:
            raise HTTPException(
                status_code=400,
                detail=f"Вопрос {answer.question_id} не найден",
            )

        answers_for_grading.append({
            "question_id": answer.question_id,
            "question": question.text,
            "max_score": question.max_score,
            "chunks": question.chunks,
            "answer_text": answer.answer_text,
        })

    try:
        return grade_attempt(answers_for_grading)

    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail=str(e),
        )