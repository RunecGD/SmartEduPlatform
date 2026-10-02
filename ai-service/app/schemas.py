from pydantic import AliasChoices, BaseModel, ConfigDict, Field, model_validator

class Request(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

class IndexRequest(Request):
    object_key: str = Field(min_length=1, max_length=1024)

class AskRequest(Request):
    question: str = Field(min_length=1, max_length=10000)
    lesson_id: int = Field(gt=0, validation_alias=AliasChoices("lesson_id", "lessonId"))

class GenerateExamRequest(Request):
    lesson_id: int = Field(gt=0, validation_alias=AliasChoices("lesson_id", "lessonId"))
    count: int = Field(default=5, ge=1, le=20)

class GradeQuestion(Request):
    id: int = Field(gt=0)
    text: str = Field(min_length=1, max_length=10000)
    max_score: int = Field(ge=1, le=100, validation_alias=AliasChoices("max_score", "maxScore"))
    chunks: list[str] = Field(default_factory=list, max_length=20)
    @model_validator(mode="after")
    def limit_context(self):
        if sum(len(c) for c in self.chunks)>50000: raise ValueError("Контекст слишком длинный")
        return self

class GradeAnswer(Request):
    question_id: int = Field(gt=0, validation_alias=AliasChoices("question_id", "questionId"))
    answer_text: str = Field(max_length=10000, validation_alias=AliasChoices("answer_text", "answerText"))

class GradeAttemptRequest(Request):
    questions: list[GradeQuestion] = Field(min_length=1, max_length=20)
    answers: list[GradeAnswer] = Field(max_length=20)
    @model_validator(mode="after")
    def validate_ids(self):
        ids=[q.id for q in self.questions]
        answer_ids=[a.question_id for a in self.answers]
        if len(set(ids))!=len(ids) or len(set(answer_ids))!=len(answer_ids):
            raise ValueError("Повторяющийся questionId")
        if set(answer_ids)-set(ids): raise ValueError("Ответ на посторонний вопрос")
        return self

class GeneratedQuestion(BaseModel):
    model_config=ConfigDict(extra="forbid", strict=True, str_strip_whitespace=True)
    question: str = Field(min_length=1,max_length=10000)
    max_score: int = Field(ge=1,le=10)

class GeneratedExam(BaseModel):
    model_config=ConfigDict(extra="forbid",strict=True)
    questions: list[GeneratedQuestion] = Field(min_length=1,max_length=20)

class GradeResult(BaseModel):
    model_config=ConfigDict(extra="forbid",strict=True)
    score: int = Field(ge=0,le=100)
    feedback: str = Field(max_length=10000)
