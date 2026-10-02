import { z } from 'zod';
import { studentProgressSchema, teacherAnalyticsSchema, askSchema } from '../types/core';
import { core, id } from './axios';
import type { AskRequest } from '../types/requests';
export const learningApi = {
  progress: async (userId: number, signal?: AbortSignal) => z.array(studentProgressSchema).parse(await core({ url: `/api/v1/progress/student/${id(userId)}`, signal })),
  analytics: async (courseId: number, signal?: AbortSignal) => teacherAnalyticsSchema.parse(await core({ url: `/api/v1/analytics/courses/${id(courseId)}`, signal })),
  ask: async (lessonId: number, data: AskRequest) => askSchema.parse(await core({ method: 'POST', url: `/api/v1/ask/${id(lessonId)}/lesson`, data, timeout: 240000 })),
};
