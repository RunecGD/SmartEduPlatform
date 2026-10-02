import { z } from 'zod';
import { examSchema, examQuestionSchema, examAttemptSchema } from '../types/core';
import { core, id } from './axios';
import type { ExamRequest, ExamAnswerRequest } from '../types/requests';
// "exems" is the exact controller mapping; do not normalize it to "exams".
export const examsApi = {
  create: async (lessonId: number, data: ExamRequest) => examSchema.parse(await core({ method: 'POST', url: `/api/v1/exems/lessons/${id(lessonId)}`, data })),
  generate: async ({examId,count}: {examId:number;count:number}) => z.array(examQuestionSchema).parse(await core({ method: 'POST', url: `/api/v1/exems/${id(examId)}/generate`, params: {count}, timeout: 240000 })),
  startAttempt: async (examId: number) => examAttemptSchema.parse(await core({ method: 'POST', url: `/api/v1/exems/${id(examId)}/attempts` })),
  submitAttempt: async (attemptId: number, answers?: ExamAnswerRequest[]) => examAttemptSchema.parse(await core({ method: 'POST', url: `/api/v1/exems/attempts/${id(attemptId)}/submit`, data: answers, timeout: 240000 })),
  getAttempt: async (attemptId: number, signal?: AbortSignal) => examAttemptSchema.parse(await core({ url: `/api/v1/exems/attempts/${id(attemptId)}`, signal })),
};
