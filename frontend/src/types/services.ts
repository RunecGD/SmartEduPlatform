import type { UserResponse } from './core';
export type ExecutionStatus = 'SUCCESS' | 'COMPILATION_ERROR' | 'RUNTIME_ERROR' | 'TIME_LIMIT_EXCEEDED' | 'INTERNAL_ERROR';
export interface CodeExecutionRequest { language: string; code: string; stdin?: string | null }
export interface CodeExecutionResponse { status: ExecutionStatus; stdout: string | null; stderr: string | null; exitCode: number | null; executionTimeMs: number | null }
export type NotificationType = 'EXAM_PASSED' | 'USER_ENROLLED' | 'FORUM_REPLY';
export interface NotificationResponse { id: string; userId: number; type: NotificationType; message: string; read: boolean | null; createdAt: string | null }
export interface ExamPassedEvent { userId: number; examId: number; attemptId: number; score: number }
export interface ForumAnsweredEvent { userId: number; questionId: number; answerId: number }
export interface ForumReplyEvent { userId: number; topicId: number; postId: number }
export interface UserEnrolledEvent { userId: number; courseId: number }
export interface LoginResponse { token: string }
export interface RegisterResponse { token: string; user: UserResponse }
export interface MaterialDownloadResponse { url: string }
