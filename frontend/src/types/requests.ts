import type { LessonType } from './core';
/** Exact JSON contracts from supplied core DTOs. Missing enums remain explicit generics. */
export interface LoginRequest { email: string; password: string }
export interface UserRequest extends LoginRequest { fullName: string }
export interface CourseRequest { title: string; category?: string | null; description?: string | null }
export interface CourseModuleRequest { title: string; orderIndex: number }
export interface LessonRequest<TLessonType extends LessonType = LessonType> { type: TLessonType; title: string; content?: string | null; orderIndex: number }
export interface LessonProgressRequest { lessonId: number; score?: number | null }
export interface ExamRequest { title: string | null; timeLimitMinutes: number | null }
export interface ExamAnswerRequest { questionId: number; answerText: string | null }
export interface ForumTopicRequest { title: string }
export interface ForumPostRequest { content: string }
export interface PostReactionRequest { reaction: string }
export interface AskRequest { question: string; lesson_id?: number | null }
export interface EnrollmentRequest { courseId: number }
// Internal Core → AI contracts. Never sent directly from the frontend to the AI service.
export interface AiGenerateExamRequest { lesson_id: number; count: number }
export interface AiGradeQuestion { id: number; text: string; maxScore: number; chunks: string[] }
export interface AiGradeAttemptRequest { questions: AiGradeQuestion[]; answers: ExamAnswerRequest[] }
