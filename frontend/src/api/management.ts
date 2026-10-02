import { z } from 'zod';
import { core, id } from './axios';
import { courseSchema, userSchema, type Role, type CourseStatus } from '../types/core';
import { groupSchema, studentResultsSchema, aiLessonStatusSchema, type GroupRequest } from '../types/management';
export const managementApi = {
  users: async () => z.array(userSchema).parse(await core({ url: '/api/v1/admin/users' })),
  updateUser: async (userId: number, data: { fullName: string; role: Role; isEnabled: boolean }) => userSchema.parse(await core({ method: 'PATCH', url: `/api/v1/admin/users/${id(userId)}`, data })),
  courses: async () => z.array(courseSchema).parse(await core({ url: '/api/v1/admin/courses' })),
  updateCourse: async (courseId: number, data: { teacherId: number; status: CourseStatus }) => courseSchema.parse(await core({ method: 'PATCH', url: `/api/v1/admin/courses/${id(courseId)}`, data })),
  groups: async () => z.array(groupSchema).parse(await core({ url: '/api/v1/groups' })),
  saveGroup: async (groupId: number | null, data: GroupRequest) => groupSchema.parse(await core({ method: groupId ? 'PUT' : 'POST', url: `/api/v1/admin/groups${groupId ? `/${id(groupId)}` : ''}`, data })),
  courseResults: async (courseId: number) => z.array(studentResultsSchema).parse(await core({ url: `/api/v1/reports/courses/${id(courseId)}` })),
  groupResults: async (groupId: number) => z.array(studentResultsSchema).parse(await core({ url: `/api/v1/reports/groups/${id(groupId)}` })),
  aiStatus: async (lessonId: number) => aiLessonStatusSchema.parse(await core({ url: `/api/v1/lessons/${id(lessonId)}/ai-status` })),
};
