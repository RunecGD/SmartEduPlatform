import { z } from 'zod';
import { courseSchema, courseSearchSchema, enrollmentSchema, courseModuleSchema, lessonSchema, lessonProgressSchema, type LessonType } from '../types/core';
import { core, id } from './axios';
import type { CourseRequest, CourseModuleRequest, LessonRequest, LessonProgressRequest } from '../types/requests';
export const coursesApi = {
  mine: async (signal?: AbortSignal) => z.array(courseSchema).parse(await core({url:'/api/v1/courses/mine',signal})),
  catalog: async (signal?: AbortSignal) => z.array(courseSchema).parse(await core({ url: '/api/v1/courses', signal })),
  search: async (query?: string, signal?: AbortSignal) => z.array(courseSearchSchema).parse(await core({ url: '/api/v1/courses/search', params: { query }, signal })),
  get: async (courseId: number, signal?: AbortSignal) => courseSchema.parse(await core({ url: `/api/v1/courses/${id(courseId)}`, signal })),
  create: async (data: CourseRequest) => courseSchema.parse(await core({ method: 'POST', url: '/api/v1/courses', data })),
  publish: async (courseId: number) => courseSchema.parse(await core({ method: 'POST', url: `/api/v1/courses/${id(courseId)}/publish` })),
  enroll: async (courseId: number) => enrollmentSchema.parse(await core({ method: 'POST', url: `/api/v1/enrollment/${id(courseId)}` })),
  addModule: async (courseId: number, data: CourseModuleRequest) => courseModuleSchema.parse(await core({ method: 'POST', url: `/api/v1/course_modules/${id(courseId)}`, data })),
  addLesson: async <T extends LessonType>(moduleId: number, data: LessonRequest<T>) => lessonSchema.parse(await core({ method: 'POST', url: `/api/v1/lessons/modules/${id(moduleId)}`, data })),
  completeLesson: async (data: LessonProgressRequest) => lessonProgressSchema.parse(await core({ method: 'POST', url: '/api/v1/lesson_progress/complete', data })),
};
