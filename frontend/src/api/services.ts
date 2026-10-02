import { z } from 'zod';
import { request, id } from './axios';
import type { CodeExecutionRequest } from '../types/services';
export const notificationSchema = z.object({ id: z.string(), userId: z.number().int(), type: z.enum(['EXAM_PASSED','USER_ENROLLED','FORUM_REPLY']), message: z.string(), read: z.boolean().nullable(), createdAt: z.string().nullable() });
const executionSchema = z.object({ status: z.enum(['SUCCESS','COMPILATION_ERROR','RUNTIME_ERROR','TIME_LIMIT_EXCEEDED','INTERNAL_ERROR']), stdout: z.string().nullable(), stderr: z.string().nullable(), exitCode: z.number().int().nullable(), executionTimeMs: z.number().nullable() });
export const notificationsApi = {
  list: async (userId: number, signal?: AbortSignal) => z.array(notificationSchema).parse(await request('notifications', { url: `/api/v1/notifications/user/${id(userId)}`, signal })),
  markRead: async (notificationId: string, userId: number) => notificationSchema.parse(await request('notifications', { method: 'PATCH', url: `/api/v1/notifications/${id(notificationId)}/read`, params: { userId } })),
};
export const executionApi = {
  execute: async (data: CodeExecutionRequest) => executionSchema.parse(await request('execution', { method: 'POST', url: '/api/v1/code/execute', data, timeout: 120000 })),
};
