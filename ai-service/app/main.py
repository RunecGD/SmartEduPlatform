import logging
import secrets
import boto3
from botocore.exceptions import BotoCoreError, ClientError
from fastapi import Depends, FastAPI, Header, HTTPException
from openai import APIError
from pydantic import ValidationError
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
import httpx
from app.config import settings
from app.db import engine
from app.schemas import IndexRequest, AskRequest, GenerateExamRequest, GradeAttemptRequest
from app.ingest import index_material
from app.rag import ask as rag_ask
from app.examiner import generate_exam, grade_attempt

log=logging.getLogger(__name__)
def service_auth(x_service_token: str | None = Header(default=None)):
    if x_service_token is None or not secrets.compare_digest(
            x_service_token.encode(),settings.ai_service_token.get_secret_value().encode()):
        raise HTTPException(401,"Требуется авторизация сервиса")

app=FastAPI(title="SmartEdu AI Service",version="0.4.0",docs_url=None,redoc_url=None,openapi_url=None)

@app.get("/health")
def health():
    try:
        with engine.connect() as conn: conn.execute(text("SELECT 1"))
    except SQLAlchemyError:
        raise HTTPException(503,"База данных недоступна")
    return {"status":"ok","db":"connected","llm_model":settings.openrouter_model,"embed_model":settings.embed_model}

def invoke(operation,*args):
    try: return operation(*args)
    except HTTPException: raise
    except ValidationError: raise HTTPException(502,"AI вернул некорректный результат")
    except ValueError: raise HTTPException(422,"Материалы или результат AI не подходят для обработки")
    except (APIError,httpx.HTTPError,BotoCoreError,ClientError):
        raise HTTPException(502,"Внешний сервис временно недоступен")
    except SQLAlchemyError: raise HTTPException(503,"База данных недоступна")
    except Exception as ex:
        log.error("AI operation failed (%s)",type(ex).__name__)
        raise HTTPException(500,"Не удалось обработать запрос")

def index_object(req):
    with engine.connect() as conn:
        row=conn.execute(text("SELECT id,file_name,content_type FROM materials WHERE object_key=:key"),
                         {"key":req.object_key}).fetchone()
    if row is None: raise HTTPException(404,"Материал не найден")
    s3=boto3.client("s3",endpoint_url=settings.minio_url,aws_access_key_id=settings.minio_access_key,
                   aws_secret_access_key=settings.minio_secret_key.get_secret_value(),region_name="us-east-1")
    response=s3.get_object(Bucket=settings.minio_bucket,Key=req.object_key)
    body=response["Body"]
    try:
        if response.get("ContentLength",0)>settings.max_file_bytes: raise HTTPException(413,"Файл слишком большой")
        content=body.read(settings.max_file_bytes+1)
        if len(content)>settings.max_file_bytes: raise HTTPException(413,"Файл слишком большой")
    finally:
        body.close()
        s3.close()
    count=index_material(row.id,content,row.content_type,row.file_name)
    return {"material_id":row.id,"chunks_indexed":count}

@app.post("/index",dependencies=[Depends(service_auth)])
def index(req: IndexRequest): return invoke(index_object,req)

@app.post("/ask",dependencies=[Depends(service_auth)])
def ask(req: AskRequest): return invoke(rag_ask,req.question,req.lesson_id)

@app.post("/exams/generate",dependencies=[Depends(service_auth)])
def generate(req: GenerateExamRequest): return invoke(generate_exam,req.lesson_id,req.count)

@app.post("/exams/grade",dependencies=[Depends(service_auth)])
def grade(req: GradeAttemptRequest):
    answers={a.question_id:a.answer_text for a in req.answers}
    rows=[{"question_id":q.id,"question":q.text,"max_score":q.max_score,
           "chunks":q.chunks,"answer_text":answers.get(q.id,"")} for q in req.questions]
    return invoke(grade_attempt,rows)
