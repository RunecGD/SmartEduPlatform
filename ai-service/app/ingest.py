import json
import math
import httpx
from sqlalchemy import text
from app.config import settings
from app.db import engine

def chunk_text(text_content: str,size: int=500,overlap: int=50)->list[str]:
    if size<=0 or overlap<0 or overlap>=size: raise ValueError("Некорректные параметры чанков")
    normalized=" ".join(text_content.split())
    if not normalized: return []
    chunks=[]
    start=0
    while start<len(normalized):
        chunks.append(normalized[start:start+size])
        if start+size>=len(normalized): break
        start+=size-overlap
    return chunks

def embed(texts: list[str])->list[list[float]]:
    if not texts: return []
    vectors=[]
    with httpx.Client(timeout=120) as client:
        for start in range(0,len(texts),64):
            batch=texts[start:start+64]
            response=client.post(settings.ollama_url.rstrip("/")+"/api/embed",
                                 json={"model":settings.embed_model,"input":batch})
            response.raise_for_status()
            received=response.json().get("embeddings")
            if not isinstance(received,list) or len(received)!=len(batch):
                raise ValueError("Число эмбеддингов не соответствует текстам")
            for vector in received:
                if not isinstance(vector,list) or len(vector)!=settings.embedding_dimensions:
                    raise ValueError("Размерность эмбеддинга не соответствует настройке")
                if any(isinstance(v,bool) or not isinstance(v,(float,int)) or not math.isfinite(v) for v in vector):
                    raise ValueError("Некорректный вектор")
                if not any(v!=0 for v in vector): raise ValueError("Нулевой вектор не подходит для cosine search")
            vectors.extend(received)
    return vectors

def index_material(material_id: int,content: bytes,content_type: str|None,file_name: str):
    from app.extractors import extract_text
    chunks=chunk_text(extract_text(content,content_type,file_name))
    if not chunks: raise ValueError("Материал не содержит текста; для скана нужен OCR")
    vectors=embed(chunks)
    if len(vectors)!=len(chunks): raise ValueError("Неполный набор векторов")
    with engine.begin() as conn:
        material=conn.execute(text("SELECT id FROM materials WHERE id=:id FOR UPDATE"),{"id":material_id}).fetchone()
        if material is None: raise ValueError("Материал удален")
        conn.execute(text("DELETE FROM material_chunks WHERE material_id=:id"),{"id":material_id})
        for index,(chunk,vector) in enumerate(zip(chunks,vectors,strict=True)):
            conn.execute(text("INSERT INTO material_chunks(material_id,chunk_index,content,embedding) "
                              "VALUES (:id,:index,:content,CAST(:vector AS vector))"),
                         {"id":material_id,"index":index,"content":chunk,"vector":json.dumps(vector,allow_nan=False)})
    return len(chunks)
