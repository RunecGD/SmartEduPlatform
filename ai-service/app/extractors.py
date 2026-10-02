from io import BytesIO
from zipfile import ZipFile
import pymupdf
from docx import Document
from app.config import settings

def extract_text(content: bytes,content_type: str|None,file_name: str)->str:
    if len(content)>settings.max_file_bytes: raise ValueError("Файл слишком большой")
    name=(file_name or "").lower()
    try:
        if name.endswith(".pdf"):
            with pymupdf.open(stream=content,filetype="pdf") as doc:
                if len(doc)>settings.max_pdf_pages: raise ValueError("Слишком много страниц")
                result=[]; size=0
                for page in doc:
                    part=page.get_text(); size+=len(part)
                    if size>settings.max_text_chars: raise ValueError("Слишком много текста")
                    result.append(part)
                extracted="\n".join(result)
        elif name.endswith(".docx"):
            with ZipFile(BytesIO(content)) as archive:
                if len(archive.infolist())>5000 or sum(i.file_size for i in archive.infolist())>50*1024*1024:
                    raise ValueError("DOCX слишком большой после распаковки")
            doc=Document(BytesIO(content))
            parts=[p.text for p in doc.paragraphs if p.text.strip()]
            parts.extend(" | ".join(c.text for c in row.cells) for table in doc.tables for row in table.rows)
            extracted="\n".join(parts)
        elif name.endswith(".txt"):
            extracted=content.decode("utf-8-sig")
        else: raise ValueError("Поддерживаются PDF, DOCX и TXT в UTF-8")
    except ValueError: raise
    except Exception as ex: raise ValueError("Файл поврежден или его формат не поддерживается") from ex
    if len(extracted)>settings.max_text_chars: raise ValueError("Слишком много текста")
    return extracted
