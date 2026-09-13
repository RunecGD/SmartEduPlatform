import fitz                      # PyMuPDF
from docx import Document
from fastapi import HTTPException


def extract_text(content: bytes, content_type: str | None, file_name: str) -> str:
    """Достаёт чистый текст из PDF/DOCX/TXT."""
    name = file_name.lower()

    if content_type == "application/pdf" or name.endswith(".pdf"):
        doc = fitz.open(stream=content, filetype="pdf")
        return "\n".join(page.get_text() for page in doc)

    if name.endswith(".docx"):
        from io import BytesIO
        doc = Document(BytesIO(content))
        return "\n".join(p.text for p in doc.paragraphs if p.text.strip())

    if name.endswith(".txt") or (content_type or "").startswith("text/"):
        return content.decode("utf-8", errors="ignore")

    raise HTTPException(422, f"Неподдерживаемый формат файла: {file_name}")