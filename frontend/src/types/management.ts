import { z } from 'zod';
import { userSchema, studentProgressSchema, attemptStatusSchema } from './core';
const id = z.number().int().positive().safe();
export const groupSchema = z.object({ id, name: z.string(), curatorId: id, curatorName: z.string(), students: z.array(userSchema) });
export const studentResultsSchema = z.object({
  userId: id, fullName: z.string(), email: z.string(),
  courses: z.array(z.object({
    progress: studentProgressSchema,
    attempts: z.array(z.object({ attemptId: id, examId: id, examTitle: z.string(), status: attemptStatusSchema,
      totalScore: z.number().int().nullable(), finishedAt: z.string().nullable() })),
  })),
});
export const aiLessonStatusSchema = z.object({ materialCount: z.number().int().nonnegative(), readyCount: z.number().int().nonnegative(), pendingCount: z.number().int().nonnegative(), retryingCount: z.number().int().nonnegative() });
export type StudyGroup = z.infer<typeof groupSchema>;
export type StudentResults = z.infer<typeof studentResultsSchema>;
export interface GroupRequest { name: string; curatorId: number; studentIds: number[] }
export const roleNames = { STUDENT: 'Студент', TEACHER: 'Преподаватель', METHODIST: 'Куратор', ADMIN: 'Администратор' } as const;
