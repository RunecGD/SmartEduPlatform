import { useQuery } from '@tanstack/react-query';
import { learningApi } from '../api/learning';
import { Link } from 'react-router-dom';
import { BookOpen, MessageSquare, BarChart3, Check, Send } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { useCourse, useEnrollment, usePublishCourse } from '../hooks/useApi';
import { useAuth } from '../context/AuthContext';
import { PageHeading, Loading, QueryError, Back, useRouteId, InvalidId, statusLabel } from '../components/Page';
import { ErrorMessage } from '../components/Feedback';
import {CourseProgram} from '../components/CourseProgram';
export function CoursePage() {const id = useRouteId('courseId');return id ? <Course key={id} id={id}/> : <InvalidId/>;}
function Course({id}: {id:number}) {
 const {user} = useAuth(); const query = useCourse(id); const enroll = useEnrollment(); const publish = usePublishCourse();
 const progress=useQuery({queryKey:['progress',user?.id],queryFn:({signal})=>learningApi.progress(user!.id,signal),enabled:user?.role==='STUDENT'});
 const enrolled=enroll.isSuccess || progress.data?.some(c=>c.courseId===id)===true;
 if(query.isLoading)return <Loading/>;if(query.error)return <QueryError error={query.error} retry={() => void query.refetch()}/>;if(!query.data)return null;
 const course = query.data;const owner = user?.role === 'ADMIN' || (user?.role === 'TEACHER' && user.id === course.teacherId);const canForum = user && ['STUDENT','TEACHER','ADMIN'].includes(user.role);
 return <><Back to="/courses">Каталог курсов</Back><PageHeading eyebrow={course.category || 'КУРС'} title={course.title}><span className="status-tag">{statusLabel(course.status)}</span></PageHeading><div className="detail-grid"><section className="panel"><h2><BookOpen size={20}/>О курсе</h2><p className="prose-text">{course.description || 'Описание пока не добавлено.'}</p><div className="course-actions">{user?.role==='STUDENT' && (enrolled ? <span className="inline-flex items-center gap-2 text-sm"><Check size={18}/>Вы записаны на курс</span> : progress.isPending ? <span className="text-sm">Проверяем запись на курс…</span> : progress.error ? <div><ErrorMessage error={progress.error}/><Button variant="outline" onClick={()=>void progress.refetch()}>Проверить запись повторно</Button></div> : course.status==='PUBLISHED' ? <Button disabled={enroll.isPending} onClick={()=>enroll.mutate(id,{onSuccess:()=>toast.success('Вы записаны на курс')})}><BookOpen/>{enroll.isPending ? 'Записываем…' : 'Записаться на курс'}</Button> : null)}{owner && course.status === 'DRAFT' && <Button disabled={publish.isPending} onClick={() => publish.mutate(id,{onSuccess: () => toast.success('Курс опубликован')})}><Send/>{publish.isPending ? 'Публикуем…' : 'Опубликовать'}</Button>}</div><ErrorMessage error={enroll.error || publish.error}/></section><aside className="panel"><h2>Пространство курса</h2>{canForum && <Link className="action-row" to={`/courses/${id}/forum`}><MessageSquare size={19}/>Обсуждения курса</Link>}{owner && <Link className="action-row" to={`/teacher/analytics/${id}`}><BarChart3 size={19}/>Аналитика</Link>}{owner && <Link className="action-row" to={`/reports/courses/${id}`}><BarChart3 size={19}/>Результаты учеников</Link>}</aside></div><CourseProgram courseId={id} owner={owner}/></>;
}
