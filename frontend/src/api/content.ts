import { z } from 'zod';
import { core,id } from './axios';
import {courseModuleSchema,lessonSchema,materialSchema,examSchema,examQuestionSchema} from '../types/core';
export const contentApi={
 modules:async(courseId:number)=>z.array(courseModuleSchema).parse(await core({url:`/api/v1/course_modules/${id(courseId)}`})),
 lessons:async(moduleId:number)=>z.array(lessonSchema).parse(await core({url:`/api/v1/lessons/modules/${id(moduleId)}`})),
 lesson:async(lessonId:number)=>lessonSchema.parse(await core({url:`/api/v1/lessons/${id(lessonId)}`})),
 materials:async(lessonId:number)=>z.array(materialSchema).parse(await core({url:`/api/v1/material/${id(lessonId)}`})),
 exams:async(lessonId:number)=>z.array(examSchema).parse(await core({url:`/api/v1/exems/lessons/${id(lessonId)}`})),
 exam:async(examId:number)=>examSchema.parse(await core({url:`/api/v1/exems/${id(examId)}`})),
 questions:async(examId:number)=>z.array(examQuestionSchema).parse(await core({url:`/api/v1/exems/${id(examId)}/questions`})),
};
