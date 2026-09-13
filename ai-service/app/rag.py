import httpx
from sqlalchemy import text

from app.config import settings
from app.db import engine


def ask(question: str, lesson_id: int) -> dict:
    # 1. эмбеддинг вопроса
    [q_vec] = __import__("app.ingest", fromlist=["embed"]).embed([question])

    # 2. поиск 5 ближайших чанков именно этого урока (косинусная близость)
    with engine.connect() as conn:
        rows = conn.execute(text("""
            SELECT mc.content, m.file_name
            FROM material_chunks mc
            JOIN materials m ON m.id = mc.material_id
            WHERE m.lesson_id = :lid
            ORDER BY mc.embedding <=> CAST(:qv AS vector)
            LIMIT 5
        """), {"lid": lesson_id, "qv": __import__("json").dumps(q_vec)}).fetchall()

    if not rows:
        return {"answer": "По этому уроку пока нет материалов — задайте вопрос преподавателю.",
                "sources": []}

    context = "\n\n".join(f"[Фрагмент из «{r.file_name}»]\n{r.content}" for r in rows)

    # 3. промпт: LLM отвечает ТОЛЬКО по контексту
    prompt = f"""Ты — тьютор образовательной платформы. Отвечай на вопрос студента,
используя ТОЛЬКО информацию из приведённых фрагментов лекции.
Если ответа в фрагментах нет — честно скажи об этом, не выдумывай.
Отвечай кратко и по делу, на русском.

Фрагменты лекции:
{context}

Вопрос студента: {question}"""

    with httpx.Client(timeout=180) as client:
        resp = client.post(f"{settings.ollama_url}/api/generate", json={
            "model": settings.llm_model,
            "prompt": prompt,
            "stream": False,
            "options": {"temperature": 0.2},   # почти детерминированные ответы
        })
        resp.raise_for_status()
        answer = resp.json()["response"].strip()

    return {"answer": answer, "sources": [r.file_name for r in rows]}