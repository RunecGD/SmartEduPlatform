import test from 'node:test';
import assert from 'node:assert/strict';
import { courseSchema, courseSearchSchema, examAttemptSchema, examWebSocketSchema, studentProgressSchema, userSchema, teacherAnalyticsSchema } from '../src/types/core.ts';
import { loginSchema, registerSchema } from '../src/schemas/auth.ts';
import { getToken, getUser, setToken } from '../src/api/session.ts';
// Isolated contract fixtures: never imported into production and never replace API data.
const user = {id:7,email:'student@example.test',fullName:'Test Student',role:'STUDENT',isEnabled:true,createdAt:null,updatedAt:null};
test('role METHODIST survives parsing and is not converted to TEACHER',()=>{
 assert.equal(userSchema.parse({...user,role:'METHODIST'}).role,'METHODIST');
 assert.equal(userSchema.safeParse({...user,role:'SUPERADMIN'}).success,false);
});
test('course and search responses have genuinely different contracts',()=>{
 const course={id:1,teacherId:2,title:'Contract fixture',category:null,description:null,status:'PUBLISHED',createdAt:null,updatedAt:null};
 assert.equal(courseSchema.parse(course).category,null);
 assert.equal(courseSearchSchema.safeParse(course).success,false);
 assert.equal(courseSearchSchema.parse({...course,teacherName:'Teacher'}).teacherName,'Teacher');
});
test('BigDecimal progress stays numeric including fractional values',()=>{
 const parsed=studentProgressSchema.parse({courseId:1,courseTitle:'Course',progressPercent:33.33,completedLessons:1,totalLessons:3,lessons:[],exams:[]});
 assert.equal(parsed.progressPercent,33.33);
 assert.equal(studentProgressSchema.safeParse({...parsed,progressPercent:'33.33'}).success,false);
});
test('ungraded attempt accepts null score and feedback without implying zero or failure',()=>{
 const attempt=examAttemptSchema.parse({attemptId:1,examId:2,startedAt:null,finishedAt:null,totalScore:null,status:'IN_PROGRESS',answers:[{questionId:3,question:'Question',answer:null,score:null,feedback:null,maxScore:10}]});
 assert.equal(attempt.totalScore,null);assert.equal(attempt.answers?.[0].score,null);
});
test('exam timer reads remainingSeconds from the message, including zero',()=>{
 assert.equal(examWebSocketSchema.parse({attemptId:1,status:'EXPIRED',remainingSeconds:0,startedAt:null,deadline:null}).remainingSeconds,0);
 assert.equal(examWebSocketSchema.safeParse({attemptId:1,status:'IN_PROGRESS',seconds:10}).success,false);
});
test('analytics allows absent metrics as null rather than fabricated zeros',()=>{
 assert.equal(teacherAnalyticsSchema.parse({courseId:1,courseTitle:'Course',totalStudents:0,completedStudents:0,averageProgress:null,averageExamScore:null,lessons:[]}).averageExamScore,null);
});
test('login does not impose registration password length constraints',()=>{
 assert.equal(loginSchema.safeParse({email:'a@example.test',password:'old'}).success,true);
 assert.equal(registerSchema.safeParse({...user,password:'old'}).success,false);
});
test('identity is tied to a token and never leaks into another login',()=>{
 const data=new Map<string,string>();
 Object.defineProperty(globalThis,'sessionStorage',{configurable:true,value:{getItem:(key:string)=>data.get(key)??null,setItem:(key:string,value:string)=>data.set(key,value),removeItem:(key:string)=>data.delete(key)}});
 Object.defineProperty(globalThis,'window',{configurable:true,value:{dispatchEvent:()=>true}});
 setToken('registration-token',userSchema.parse(user));assert.equal(getUser()?.id,7);
 setToken('different-login-token');assert.equal(getUser(),null);assert.equal(getToken(),'different-login-token');
 setToken(null);assert.equal(getToken(),null);assert.equal(getUser(),null);
});

test('ProgressService placeholders accept null id and separate repeated exam attempts',()=>{
 const parsed=studentProgressSchema.parse({courseId:1,courseTitle:'Course',progressPercent:0,completedLessons:0,totalLessons:1,lessons:[{id:null,enrollmentId:2,lessonId:3,completedAt:null,score:null}],exams:[{examId:5,examTitle:'Exam',score:4,status:'FINISHED'},{examId:5,examTitle:'Exam',score:8,status:'FINISHED'}]});
 assert.equal(parsed.lessons?.[0].id,null);assert.equal(parsed.lessons?.[0].completedAt,null);assert.equal(parsed.exams?.length,2);
});

test('group membership and attempt IDs are preserved for scoped results', async()=>{
 const {groupSchema,studentResultsSchema}=await import('../src/types/management.ts');
 assert.equal(groupSchema.parse({id:1,name:'Group',curatorId:2,curatorName:'Curator',students:[user]}).students[0].role,'STUDENT');
 const data=studentResultsSchema.parse({userId:7,fullName:'Student',email:'a@example.test',courses:[{progress:{courseId:1,courseTitle:'Course',progressPercent:33.33,completedLessons:1,totalLessons:3,lessons:[],exams:[]},attempts:[{attemptId:10,examId:20,examTitle:'Exam',status:'EXPIRED',totalScore:null,finishedAt:null}]}]});
 assert.equal(data.courses[0].attempts[0].attemptId,10);
 assert.equal(data.courses[0].attempts[0].totalScore,null);
 assert.equal(studentResultsSchema.parse({userId:7,fullName:'Student',email:'a@example.test',courses:[]}).courses.length,0);
});
test('AI readiness distinguishes retrying uploads from ready materials', async()=>{
 const {aiLessonStatusSchema}=await import('../src/types/management.ts');
 const data=aiLessonStatusSchema.parse({materialCount:2,readyCount:0,pendingCount:1,retryingCount:1});
 assert.equal(data.readyCount,0);assert.equal(data.retryingCount,1);
 assert.equal(aiLessonStatusSchema.safeParse({...data,readyCount:-1}).success,false);
});
