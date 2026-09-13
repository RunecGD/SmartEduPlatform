import json
import httpx
from sqlalchemy import text

from app.config import settings
from app.db import engine


def chunk_text(text_content: str, size: int = 500, overlap: int = 50) -> list[str]:
    """Режет текст на перекрывающиеся куски по ~500 символов."""
    text_content = " ".join(text_content.split())      # схлопнуть переносы
    chunks, start = [], 0
    while start < len(text_content):
        chunks.append(text_content[start:start + size])
        start += size - overlap
    return [c for c in chunks if len(c.strip()) > 50]  # отбросить мусор


def embed(texts: list[str]) -> list[list[float]]:
    """Эмбеддинги через Ollama (пакетно)."""
    with httpx.Client(timeout=120) as client:
        resp = client.post(f"{settings.ollama_url}/api/embed", json={
            "model": settings.embed_model,
            "input": texts,
        })
        resp.raise_for_status()
        return resp.json()["embeddings"]


def index_material(material_id: int, content: bytes, content_type: str, file_name: str):
    """Полный пайплайн: файл -> текст -> чанки -> эмбеддинги -> material_chunks."""
    from app.extractors import extract_text

    full_text = extract_text(content, content_type, file_name)
    chunks = chunk_text(full_text)
    if not chunks:
        return 0

    vectors = embed(chunks)

    with engine.begin() as conn:                       # begin() = транзакция с commit
        conn.execute(text("DELETE FROM material_chunks WHERE material_id = :mid"),
                     {"mid": material_id})             # переиндексация: старое удаляем
        for i, (chunk, vec) in enumerate(zip(chunks, vectors)):
            conn.execute(text("""
                INSERT INTO material_chunks (material_id, chunk_index, content, embedding)
                VALUES (:mid, :idx, :content, CAST(:vec AS vector))
            """), {
                "mid": material_id, "idx": i,
                "content": chunk, "vec": json.dumps(vec),
            })
    return len(chunks)