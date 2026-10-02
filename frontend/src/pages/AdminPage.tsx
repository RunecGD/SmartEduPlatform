import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { managementApi } from '../api/management';
import { PageHeading, Loading, QueryError, statusLabel } from '../components/Page';
import { ErrorMessage, Unavailable } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';
import { roleNames, type StudyGroup, type GroupRequest } from '../types/management';
import type { Role, UserResponse, CourseResponse, CourseStatus } from '../types/core';

export function AdminPage() {
  const [section, setSection] = useState<'users' | 'groups' | 'courses'>('users');
  return <><PageHeading eyebrow="АДМИНИСТРАТОРУ" title="Управление платформой" description="Пользователи, учебные группы и курсы."/>
    <nav aria-label="Разделы управления" className="flex flex-wrap gap-2 mb-6">{([['users','Пользователи'],['groups','Группы'],['courses','Курсы']] as const).map(([key,label]) => <Button key={key} variant={section === key ? 'default' : 'outline'} aria-pressed={section === key} onClick={() => setSection(key)}>{label}</Button>)}</nav>
    {section === 'users' ? <UsersPanel/> : section === 'groups' ? <GroupsPanel/> : <CoursesPanel/>}</>;
}
function useAdminUsers() { return useQuery({queryKey: ['admin','users'], queryFn: managementApi.users}); }
function UsersPanel() {
  const query = useAdminUsers();
  const [search, setSearch] = useState('');
  if (query.isLoading) return <Loading/>;
  if (query.error) return <QueryError error={query.error} retry={() => void query.refetch()}/>;
  const users = (query.data ?? []).filter(user => `${user.fullName} ${user.email}`.toLowerCase().includes(search.toLowerCase()));
  return <><Input className="max-w-lg mb-4" placeholder="Имя или email" aria-label="Поиск пользователей" value={search} onChange={e => setSearch(e.target.value)}/>
    <p className="text-sm text-muted-foreground mb-4">После регистрации пользователь становится студентом. Здесь можно назначить преподавателя или куратора. Перед сменой роли снимите назначения в группах и курсах.</p>
    {!users.length ? <Unavailable title="Пользователи не найдены" message="Измените поисковый запрос."/> : <div className="space-y-4">{users.map(user => <UserEditor key={`${user.id}-${user.updatedAt}`} user={user}/>)}</div>}</>;
}
function UserEditor({user}: {user: UserResponse}) {
  const {user: current} = useAuth();
  const cache = useQueryClient();
  const [name, setName] = useState(user.fullName), [role, setRole] = useState<Role>(user.role), [enabled, setEnabled] = useState(user.isEnabled ?? true);
  const mutation = useMutation({mutationFn: () => managementApi.updateUser(user.id, {fullName: name.trim(), role, isEnabled: enabled}), onSuccess: () => {void cache.invalidateQueries({queryKey:['admin']}); void cache.invalidateQueries({queryKey:['groups']}); toast.success('Пользователь обновлён');}});
  const self = current?.id === user.id;
  return <form className="panel" onSubmit={e => {e.preventDefault(); mutation.mutate();}}><p className="text-sm text-muted-foreground mb-3">{user.email}{self ? ' · Ваш аккаунт' : ''}</p><div className="management-form">
    <label>Имя<Input value={name} onChange={e => setName(e.target.value)} maxLength={100} required/></label>
    <label>Роль<select className="management-select" value={role} onChange={e => setRole(e.target.value as Role)} disabled={self}>{Object.entries(roleNames).map(([key,label]) => <option key={key} value={key}>{label}</option>)}</select></label>
    <label className="checkbox-label"><input type="checkbox" checked={enabled} disabled={self} onChange={e => setEnabled(e.target.checked)}/>Активен</label>
    <Button type="submit" disabled={mutation.isPending || !name.trim()}>Сохранить</Button></div><ErrorMessage error={mutation.error}/></form>;
}
function GroupsPanel() {
  const query = useQuery({queryKey:['groups'],queryFn:managementApi.groups});
  const users = useAdminUsers();
  const [editing, setEditing] = useState<StudyGroup | 'new' | null>(null);
  if (query.isLoading || users.isLoading) return <Loading/>;
  if (query.error || users.error) return <QueryError error={query.error || users.error} retry={() => {void query.refetch(); void users.refetch();}}/>;
  return <><div className="flex justify-between items-center mb-4"><h2 className="text-xl font-semibold">Учебные группы</h2><Button onClick={() => setEditing('new')}>Создать группу</Button></div>
    {editing && <GroupEditor key={editing === 'new' ? 'new' : editing.id} group={editing === 'new' ? undefined : editing} groups={query.data ?? []} users={users.data ?? []} close={() => setEditing(null)}/>}
    {!query.data?.length && !editing ? <Unavailable title="Групп пока нет" message="Создайте группу, выберите куратора и учеников."/> : <div className="space-y-4">{query.data?.map(group => <section key={group.id} className="panel"><div className="section-heading"><div><h2>{group.name}</h2><p className="text-sm text-muted-foreground">{group.curatorName} · Учеников: {group.students.length}</p></div><Button variant="outline" onClick={() => setEditing(group)}>Изменить</Button></div><Link className="text-primary underline" to={`/groups/${group.id}/results`}>Результаты учеников</Link></section>)}</div>}</>;
}
function GroupEditor({group, groups, users, close}: {group?:StudyGroup; groups:StudyGroup[]; users:UserResponse[]; close:()=>void}) {
  const cache=useQueryClient();
  const [name,setName]=useState(group?.name ?? ''), [curatorId,setCuratorId]=useState(group?.curatorId ?? 0), [studentIds,setStudentIds]=useState(group?.students.map(s=>s.id) ?? []), [search,setSearch]=useState('');
  const assigned = new Map(groups.filter(g=>g.id!==group?.id).flatMap(g=>g.students.map(s=>[s.id,g.name] as const)));
  const students=users.filter(u=>u.role==='STUDENT' && `${u.fullName} ${u.email}`.toLowerCase().includes(search.toLowerCase()));
  const mutation=useMutation({mutationFn:(data:GroupRequest)=>managementApi.saveGroup(group?.id ?? null,data),onSuccess:()=>{void cache.invalidateQueries({queryKey:['groups']});void cache.invalidateQueries({queryKey:['results']});toast.success('Группа сохранена');close();}});
  return <form className="panel mb-5 space-y-4" onSubmit={e=>{e.preventDefault();mutation.mutate({name:name.trim(),curatorId,studentIds});}}><h2>{group ? 'Изменить группу' : 'Новая группа'}</h2>
    <div className="management-form"><label>Название<Input value={name} onChange={e=>setName(e.target.value)} maxLength={120} required/></label><label>Куратор<select className="management-select" value={curatorId} onChange={e=>setCuratorId(Number(e.target.value))} required><option value={0} disabled>Выберите куратора</option>{users.filter(u=>u.role==='METHODIST' && (u.isEnabled || u.id===group?.curatorId)).map(u=><option disabled={!u.isEnabled} key={u.id} value={u.id}>{u.fullName}{!u.isEnabled ? ' (отключён)' : ''}</option>)}</select></label></div>
    <p className="text-sm text-muted-foreground">Один ученик состоит в одной группе. Для перевода сначала уберите его из предыдущей группы и сохраните изменения.</p>
    <Input aria-label="Поиск учеников для группы" placeholder="Найти ученика" value={search} onChange={e=>setSearch(e.target.value)}/>
    <fieldset className="student-picker"><legend className="text-sm mb-2">Ученики · выбрано {studentIds.length}</legend>{students.map(s=><label key={s.id} className="checkbox-label"><input type="checkbox" disabled={assigned.has(s.id)} checked={studentIds.includes(s.id)} onChange={e=>setStudentIds(e.target.checked ? [...studentIds,s.id] : studentIds.filter(id=>id!==s.id))}/><span>{s.fullName} · {s.email}{assigned.has(s.id) ? ` · ${assigned.get(s.id)}` : ''}{!s.isEnabled ? ' · аккаунт отключён' : ''}</span></label>)}{!students.length && <p>Ученики не найдены.</p>}</fieldset>
    <ErrorMessage error={mutation.error}/><div className="flex gap-2"><Button type="submit" disabled={!curatorId || !name.trim() || mutation.isPending}>Сохранить группу</Button><Button type="button" variant="outline" onClick={close}>Отмена</Button></div></form>;
}
function CoursesPanel() {
  const query=useQuery({queryKey:['admin','courses'],queryFn:managementApi.courses});
  const users=useAdminUsers();
  if(query.isLoading || users.isLoading)return <Loading/>;
  if(query.error || users.error)return <QueryError error={query.error || users.error} retry={()=>{void query.refetch();void users.refetch();}}/>;
  return <div className="space-y-4">{query.data?.map(course=><CourseEditor key={`${course.id}-${course.updatedAt}`} course={course} users={users.data ?? []}/>)}{!query.data?.length && <Unavailable title="Курсов пока нет" message="Преподаватели смогут создать курсы после назначения роли."/>}</div>;
}
function CourseEditor({course,users}:{course:CourseResponse;users:UserResponse[]}) {
  const cache=useQueryClient();
  const [teacherId,setTeacherId]=useState(course.teacherId),[status,setStatus]=useState<CourseStatus>(course.status);
  const mutation=useMutation({mutationFn:()=>managementApi.updateCourse(course.id,{teacherId,status}),onSuccess:()=>{void cache.invalidateQueries({queryKey:['admin','courses']});void cache.invalidateQueries({queryKey:['courses']});toast.success('Курс обновлён');}});
  return <form className="panel" onSubmit={e=>{e.preventDefault();mutation.mutate();}}><div className="section-heading"><h2>{course.title}</h2><Link className="text-primary underline" to={`/courses/${course.id}`}>Открыть курс</Link></div>
    <div className="management-form"><label>Преподаватель<select className="management-select" value={teacherId} onChange={e=>setTeacherId(Number(e.target.value))}>{users.filter(u=>u.role==='TEACHER' && (u.isEnabled || u.id===course.teacherId)).map(u=><option key={u.id} value={u.id} disabled={!u.isEnabled}>{u.fullName}{!u.isEnabled ? ' (отключён)' : ''}</option>)}</select></label><label>Статус<select className="management-select" value={status} onChange={e=>setStatus(e.target.value as CourseStatus)}>{(['DRAFT','PUBLISHED','ARCHIVED'] as const).map(s=><option key={s} value={s}>{statusLabel(s)}</option>)}</select></label><Button type="submit" disabled={mutation.isPending}>Сохранить</Button></div>
    <ErrorMessage error={mutation.error}/><Link className="text-primary underline text-sm inline-block mt-4" to={`/reports/courses/${course.id}`}>Результаты учеников</Link></form>;
}
