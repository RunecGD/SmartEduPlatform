import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, Search, RefreshCw, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { useCourses, useMyCourses, useSearchCourses } from '../hooks/useApi';
import { useAuth } from '../context/AuthContext';
import { ErrorMessage, Unavailable } from '../components/Feedback';
import { PageHeading, statusLabel } from '../components/Page';
export function CoursesPage({teacher = false}: {teacher?:boolean}) {
  const {user} = useAuth(); const [search,setSearch] = useState('');const [debounced,setDebounced] = useState(''); const [category,setCategory] = useState('all');
  useEffect(() => {const timer = setTimeout(() => setDebounced(search.trim()),300);return () => clearTimeout(timer);},[search]);
  const catalog = useCourses(!teacher);const mine=useMyCourses(teacher);const searched = useSearchCourses(debounced,!!debounced && !teacher);
  const query = teacher ? mine : debounced ? searched : catalog;
  const courses = (query.data ?? []).filter(c => (!teacher || c.teacherId === user?.id) && (category === 'all' || c.category === category) && (!teacher || c.title.toLowerCase().includes(debounced.toLowerCase())));
  const categories = [...new Set(((teacher ? mine : catalog).data ?? []).flatMap(c => c.category ? [c.category] : []))].sort();
  return <><PageHeading eyebrow={teacher ? 'ПРЕПОДАВАТЕЛЮ' : 'ОБУЧЕНИЕ'} title={teacher ? 'Мои курсы' : 'Каталог курсов'} description={teacher ? 'Черновики и опубликованные курсы.' : 'Выберите направление и сделайте следующий шаг.'}>{teacher ? <Button asChild><Link to="/teacher/courses/create"><Plus/>Создать курс</Link></Button> : <Button variant="outline" onClick={() => void query.refetch()} disabled={query.isFetching}><RefreshCw className={query.isFetching ? 'animate-spin' : ''}/>Обновить</Button>}</PageHeading>
    <div className="catalog-toolbar"><div className="search-field"><Search size={18}/><Input value={search} onChange={e => setSearch(e.target.value)} placeholder="Найти курс" aria-label="Поиск курсов"/></div><Select value={category} onValueChange={setCategory}><SelectTrigger className="w-52 h-11"><SelectValue placeholder="Категория"/></SelectTrigger><SelectContent><SelectItem value="all">Все категории</SelectItem>{categories.map(c => <SelectItem value={c} key={c}>{c}</SelectItem>)}</SelectContent></Select></div>
    {query.isFetching && <p role="status" className="text-sm text-muted-foreground mb-3">Обновляем каталог…</p>}
    {query.isLoading ? <div className="course-grid">{[0,1,2].map(i => <Skeleton key={i} className="h-72"/>)}</div> : query.isError ? <><ErrorMessage error={query.error}/><Unavailable title="Не удалось загрузить курсы" message="Попробуйте обновить каталог позже."/></> : !courses.length ? <Unavailable title={debounced || category !== 'all' ? 'Курсы не найдены' : 'Курсов пока нет'} message={debounced || category !== 'all' ? 'Измените запрос или выберите другую категорию.' : 'Опубликованные курсы появятся здесь.'}/> : <><p className="results-caption">Найдено курсов: {courses.length}</p><div className="course-grid">{courses.map(course => <Link className="course-card" key={course.id} to={`/courses/${course.id}`}><div className="course-card-top"><span className="course-icon"><BookOpen/></span><span className="status-tag">{statusLabel(course.status)}</span></div><span className="category-label">{course.category || 'Без категории'}</span><h2>{course.title}</h2><p>{course.description || 'Описание пока не добавлено.'}</p><div className="course-card-footer"><span>{'teacherName' in course && course.teacherName ? course.teacherName : `Преподаватель #${course.teacherId}`}</span><span>Открыть курс</span></div></Link>)}</div></>}
  </>;
}
