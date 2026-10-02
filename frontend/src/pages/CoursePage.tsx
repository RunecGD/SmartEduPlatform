import { Link } from 'react-router-dom';
import { BookOpen, MessageSquare, BarChart3, Check, Send } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { useCourse, useEnrollment, usePublishCourse } from '../hooks/useApi';
import { useAuth } from '../context/AuthContext';
import { PageHeading, Loading, QueryError, Back, useRouteId, InvalidId, statusLabel } from '../components/Page';
import { ErrorMessage } from '../components/Feedback';
import {CourseProgram} from '../components/CourseProgram';
export function CoursePage() {const id = useRouteId('courseId');return id ? <Course id={id}/> : <InvalidId/>;}
function Course({id}: {id:number}) {
 const {user} = useAuth(); const query = useCourse(id); const enroll = useEnrollment(); const publish = usePublishCourse();
 if(query.isLoading)return <Loading/>;if(query.error)return <QueryError error={query.error} retry={() => void query.refetch()}/>;if(!query.data)return null;
 const course = query.data;const owner = user?.role === 'ADMIN' || (user?.role === 'TEACHER' && user.id === course.teacherId);const canForum = user && ['STUDENT','TEACHER','ADMIN'].includes(user.role);
 return <><Back to="/courses">Каталог курсов</Back><PageHeading eyebrow={course.category || 'КУРС'} title={course.title} description={`Преподаватель #${course.teacherId}`}><span className="status-tag">{statusLabel(course.status)}</span></PageHeading><div className="detail-grid"><section className="panel"><h2><BookOpen size={20}/>О курсе</h2><p className="prose-text">{course.description || 'Описание пока не добавлено.'}</p><div className="course-actions">{user?.role === 'STUDENT' && course.status === 'PUBLISHED' && <Button disabled={enroll.isPending || enroll.isSuccess} onClick={() => enroll.mutate(id,{onSuccess: () => toast.success('Вы записаны на курс')})}>{enroll.isSuccess ? <Check/> : <BookOpen/>}{enroll.isSuccess ? 'Вы записаны' : enroll.isPending ? 'Записываем…' : 'Записаться на курс'}</Button>}{owner && course.status === 'DRAFT' && <Button disabled={publish.isPending} onClick={() => publish.mutate(id,{onSuccess: () => toast.success('Курс опубликован')})}><Send/>{publish.isPending ? 'Публикуем…' : 'Опубликовать'}</Button>}</div><ErrorMessage error={enroll.error || publish.error}/></section><aside className="panel"><h2>Пространство курса</h2>{canForum && <Link className="action-row" to={`/courses/${id}/forum`}><MessageSquare size={19}/>Обсуждения курса</Link>}{owner && <Link className="action-row" to={`/teacher/analytics/${id}`}><BarChart3 size={19}/>Аналитика</Link>}{owner && <Link className="action-row" to={`/reports/courses/${id}`}><BarChart3 size={19}/>Результаты учеников</Link>}</aside></div><CourseProgram courseId={id} owner={owner}/></>;
}
