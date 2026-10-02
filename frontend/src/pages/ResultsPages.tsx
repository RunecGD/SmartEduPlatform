import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Users, ArrowRight } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { managementApi } from '../api/management';
import { PageHeading, Loading, QueryError, useRouteId, InvalidId, Back, numberLabel, statusLabel, dateLabel } from '../components/Page';
import { Unavailable } from '../components/Feedback';
import type { StudentResults } from '../types/management';
export function GroupsPage() {
  const query = useQuery({ queryKey: ['groups'], queryFn: managementApi.groups });
  if (query.isLoading) return <Loading/>;
  if (query.error) return <QueryError error={query.error} retry={() => void query.refetch()}/>;
  return <><PageHeading eyebrow="КУРАТОРУ" title="Мои группы" description="Прогресс и результаты учеников ваших групп."/>
    {!query.data?.length ? <Unavailable title="Группы ещё не назначены" message="Администратор может назначить вас куратором и добавить учеников."/> :
      <div className="course-grid">{query.data.map(group => <Link key={group.id} className="course-card" to={`/groups/${group.id}/results`}>
        <div className="course-card-top"><span className="course-icon"><Users/></span><span className="status-tag">{group.students.length} учеников</span></div>
        <h2>{group.name}</h2><p>Куратор: {group.curatorName}</p><div className="course-card-footer">Посмотреть результаты <ArrowRight size={18}/></div>
      </Link>)}</div>}</>;
}
export function ResultsPage({ group = false }: { group?: boolean }) {
  const id = useRouteId(group ? 'groupId' : 'courseId');
  return id ? <Results id={id} group={group}/> : <InvalidId/>;
}
function Results({ id, group }: { id: number; group: boolean }) {
  const [search, setSearch] = useState('');
  const query = useQuery({ queryKey: ['results', group ? 'group' : 'course', id], queryFn: () => group ? managementApi.groupResults(id) : managementApi.courseResults(id) });
  const groups = useQuery({ queryKey: ['groups'], queryFn: managementApi.groups, enabled: group });
  if (query.isLoading) return <Loading/>;
  if (query.error) return <QueryError error={query.error} retry={() => void query.refetch()}/>;
  const students = query.data ?? [];
  const filtered = students.filter(s => `${s.fullName} ${s.email}`.toLowerCase().includes(search.toLowerCase()));
  const title = group ? groups.data?.find(g => g.id === id)?.name : students[0]?.courses[0]?.progress.courseTitle;
  return <><Back to={group ? '/groups' : `/courses/${id}`}>{group ? 'К группам' : 'К курсу'}</Back>
    <PageHeading eyebrow="РЕЗУЛЬТАТЫ УЧЕНИКОВ" title={title || (group ? `Группа #${id}` : `Курс #${id}`)} description={group ? 'Все курсы учеников этой группы.' : 'Прогресс и экзамены только по этому курсу.'}/>
    <Input className="max-w-lg mb-5" aria-label="Найти ученика" placeholder="Имя или email ученика" value={search} onChange={e => setSearch(e.target.value)}/>
    <p className="results-caption">Учеников: {filtered.length}</p>
    {!filtered.length ? <Unavailable title="Ученики не найдены" message={search ? 'Измените поисковый запрос.' : 'Здесь появятся ученики после назначения в группу или записи на курс.'}/> :
      <div className="space-y-5">{filtered.map(student => <StudentCard key={student.userId} student={student}/>)}</div>}</>;
}
function StudentCard({ student }: { student: StudentResults }) {
  return <section className="panel"><div className="section-heading"><div><h2>{student.fullName}</h2><p className="text-sm text-muted-foreground">{student.email}</p></div></div>
    {!student.courses.length && <p className="text-muted-foreground">Пока не записан на курсы.</p>}
    {student.courses.map(({ progress, attempts }) => <div key={progress.courseId} className="result-course">
      <div className="section-heading"><h3>{progress.courseTitle}</h3><span>{numberLabel(progress.progressPercent, '%')}</span></div>
      <progress className="w-full h-2 accent-indigo-500" aria-label={`Прогресс: ${progress.courseTitle}`} max={100} value={progress.progressPercent ?? 0}/>
      <p className="text-sm text-muted-foreground mt-2">Завершено уроков: {progress.completedLessons} из {progress.totalLessons}</p>
      <details className="mt-4"><summary className="cursor-pointer text-primary">Попытки экзаменов · {attempts.length}</summary>
        {!attempts.length ? <p className="text-sm mt-3">Экзамены ещё не начаты.</p> : <div className="table-scroll"><table className="management-table"><thead><tr><th>Экзамен</th><th>Статус</th><th>Баллы</th><th>Дата</th><th>Ответы</th></tr></thead><tbody>
          {attempts.map(attempt => <tr key={attempt.attemptId}><td>{attempt.examTitle}</td><td>{statusLabel(attempt.status)}</td><td>{numberLabel(attempt.totalScore)}</td><td>{dateLabel(attempt.finishedAt)}</td><td><Link className="text-primary underline" to={`/exams/attempts/${attempt.attemptId}`}>Открыть</Link></td></tr>)}
        </tbody></table></div>}
      </details></div>)}
  </section>;
}
