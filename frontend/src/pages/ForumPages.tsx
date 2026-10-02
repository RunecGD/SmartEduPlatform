import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { MessageSquare, Plus, Send } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { useTopics, useTopic, useCreateTopic, useCreatePost } from '../hooks/useApi';
import { ErrorMessage, Unavailable } from '../components/Feedback';
import { Back, PageHeading, Loading, QueryError, useRouteId, InvalidId, dateLabel } from '../components/Page';
const topicSchema = z.object({title:z.string().trim().min(1,'Введите название темы').max(255,'Максимум 255 символов')});
const postSchema = z.object({content:z.string().trim().min(1,'Напишите сообщение')});
export function ForumPage(){const id=useRouteId('courseId');return id ? <Forum id={id}/> : <InvalidId/>;}
function Forum({id}: {id:number}){
 const query=useTopics(id);const create=useCreateTopic(id);const navigate=useNavigate();const form=useForm<z.infer<typeof topicSchema>>({resolver:zodResolver(topicSchema),defaultValues:{title:''}});
 return <><Back to={`/courses/${id}`}>К курсу</Back><PageHeading eyebrow="СООБЩЕСТВО" title="Обсуждения курса" description="Задавайте вопросы и делитесь знаниями."/><form className="panel forum-create" onSubmit={form.handleSubmit(data=>create.mutate(data,{onSuccess:topic=>navigate(`/courses/${id}/forum/${topic.id}`)}))}><Label htmlFor="topic-title">Новая тема</Label><div className="inline-form"><Input id="topic-title" placeholder="Что вы хотите обсудить?" {...form.register('title')} aria-invalid={!!form.formState.errors.title}/><Button disabled={create.isPending}><Plus/>Создать тему</Button></div><span className="field-error">{form.formState.errors.title?.message}</span><ErrorMessage error={create.error}/></form>
 {query.isLoading ? <Loading/> : query.error ? <QueryError error={query.error} retry={()=>void query.refetch()}/> : !query.data?.length ? <Unavailable title="Начните обсуждение" message="Создайте первую тему этого курса."/> : <div className="topic-list">{query.data.map(topic=><Link className="topic-row" key={topic.id} to={`/courses/${id}/forum/${topic.id}`}><MessageSquare/><div><h2>{topic.title}</h2><p>{topic.authorName || `Участник #${topic.authorId}`} · {dateLabel(topic.createdAt)}</p></div><span>{topic.posts?.length ?? 0} ответов</span></Link>)}</div>}</>;
}
export function TopicPage(){const id=useRouteId('topicId');return id ? <Topic id={id}/> : <InvalidId/>;}
function Topic({id}: {id:number}){
 const query=useTopic(id);const reply=useCreatePost(id);const form=useForm<z.infer<typeof postSchema>>({resolver:zodResolver(postSchema),defaultValues:{content:''}});
 if(query.isLoading)return <Loading/>;if(query.error)return <QueryError error={query.error} retry={()=>void query.refetch()}/>;if(!query.data)return null;const topic=query.data;
 return <><Back to={`/courses/${topic.courseId}/forum`}>Все обсуждения</Back><PageHeading eyebrow="ОБСУЖДЕНИЕ" title={topic.title} description={`${topic.authorName || `Участник #${topic.authorId}`} · ${dateLabel(topic.createdAt)}`}/><div className="discussion">{!topic.posts?.length && <Unavailable title="Ответов пока нет" message="Будьте первым, кто присоединится к обсуждению."/>}{topic.posts?.map(post=><article className="post" key={post.id}><div className="post-meta"><span className="avatar">{post.authorName?.slice(0,1).toUpperCase() || 'У'}</span><strong>{post.authorName || `Участник #${post.authorId}`}</strong><time>{dateLabel(post.createdAt)}</time></div><p className="prose-text">{post.content}</p></article>)}</div><form className="panel mt-6" onSubmit={form.handleSubmit(data=>reply.mutate(data,{onSuccess:()=>form.reset()}))}><Label htmlFor="reply">Ваш ответ</Label><Textarea id="reply" className="my-4 min-h-32" placeholder="Напишите сообщение…" {...form.register('content')} aria-invalid={!!form.formState.errors.content}/><p className="field-error">{form.formState.errors.content?.message}</p><ErrorMessage error={reply.error}/><Button disabled={reply.isPending}><Send/>{reply.isPending ? 'Отправляем…' : 'Отправить ответ'}</Button></form></>;
}
