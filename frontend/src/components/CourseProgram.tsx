import {useState} from 'react';
import {Link} from 'react-router-dom';
import {useQuery,useMutation,useQueryClient} from '@tanstack/react-query';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {Select,SelectTrigger,SelectValue,SelectContent,SelectItem} from '@/components/ui/select';
import {contentApi} from '../api/content';
import {coursesApi} from '../api/courses';
import {ErrorMessage} from './Feedback';
import type {LessonType} from '../types/core';
export function CourseProgram({courseId,owner}:{courseId:number;owner:boolean}){
 const cache=useQueryClient();const query=useQuery({queryKey:['courses',courseId,'modules'],queryFn:()=>contentApi.modules(courseId)});
 const [title,setTitle]=useState('');
 const add=useMutation({mutationFn:()=>coursesApi.addModule(courseId,{title,orderIndex:query.data?.length ?? 0}),onSuccess:()=>{setTitle('');void cache.invalidateQueries({queryKey:['courses',courseId,'modules']});}});
 return <section className="panel mt-6"><h2>Учебная программа</h2>{query.isLoading ? <p>Загрузка…</p> : query.error ? <p className="text-muted-foreground">Чтобы открыть уроки, запишитесь на курс. Если вы уже записаны, попробуйте обновить страницу.</p> : <>{!query.data?.length && <p>Уроки пока не добавлены.</p>}{query.data?.map(module=><ModuleLessons key={module.id} id={module.id} title={module.title} courseId={courseId} owner={owner}/>)}</>}
 {owner && <form className="field mt-5" onSubmit={e=>{e.preventDefault();add.mutate();}}><Input aria-label="Название модуля" value={title} onChange={e=>setTitle(e.target.value)} placeholder="Название нового модуля" maxLength={255} required/><Button type="submit" disabled={add.isPending}>Добавить модуль</Button><ErrorMessage error={add.error}/></form>}</section>;
}
function ModuleLessons({id,title,courseId,owner}:{id:number;title:string;courseId:number;owner:boolean}){
 const cache=useQueryClient();const query=useQuery({queryKey:['lessons','module',id],queryFn:()=>contentApi.lessons(id)});
 const [name,setName]=useState('');const [content,setContent]=useState('');const [type,setType]=useState<LessonType>('TEXT');
 const add=useMutation({mutationFn:()=>coursesApi.addLesson(id,{title:name,content,type,orderIndex:query.data?.length ?? 0}),onSuccess:()=>{setName('');setContent('');void cache.invalidateQueries({queryKey:['lessons','module',id]});}});
 return <section className="border-t mt-5 pt-5"><h3 className="font-semibold mb-3">{title}</h3><ErrorMessage error={query.error}/>{query.data?.map(lesson=><Link className="action-row" key={lesson.id} to={`/courses/${courseId}/lessons/${lesson.id}`}>{lesson.title}<span className="text-muted-foreground ml-auto">{lesson.type}</span></Link>)}{owner && <form className="space-y-3 mt-4" onSubmit={e=>{e.preventDefault();add.mutate();}}><Input aria-label="Название урока" value={name} onChange={e=>setName(e.target.value)} maxLength={255} placeholder="Название урока" required/><Select value={type} onValueChange={v=>setType(v as LessonType)}><SelectTrigger aria-label="Тип урока"><SelectValue/></SelectTrigger><SelectContent>{(['TEXT','VIDEO','QUIZ','CODE'] as const).map(t=><SelectItem key={t} value={t}>{t}</SelectItem>)}</SelectContent></Select><Textarea aria-label="Содержание урока" value={content} onChange={e=>setContent(e.target.value)} placeholder="Содержание урока"/><Button type="submit" disabled={add.isPending}>Добавить урок</Button><ErrorMessage error={add.error}/></form>}</section>;
}
