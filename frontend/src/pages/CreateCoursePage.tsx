import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { useCreateCourse } from '../hooks/useApi';
import { Back, PageHeading } from '../components/Page';
import { ErrorMessage } from '../components/Feedback';
const schema=z.object({title:z.string().trim().min(1,'Введите название').max(255,'Максимум 255 символов'),category:z.string().max(100,'Максимум 100 символов'),description:z.string()});
export function CreateCoursePage(){const form=useForm<z.infer<typeof schema>>({resolver:zodResolver(schema),defaultValues:{title:'',category:'',description:''}});const create=useCreateCourse();const navigate=useNavigate();return <><Back to="/teacher/courses">К курсам</Back><PageHeading eyebrow="ПРЕПОДАВАТЕЛЮ" title="Новый курс" description="Начните с названия и описания."/><form className="panel max-w-3xl" onSubmit={form.handleSubmit(data=>create.mutate(data,{onSuccess:course=>{toast.success('Курс создан');navigate(`/courses/${course.id}`);}}))}>{(['title','category','description'] as const).map(name=><div className="field" key={name}><Label htmlFor={name}>{{title:'Название курса',category:'Категория',description:'Описание'}[name]}</Label>{name==='description' ? <Textarea id={name} className="min-h-40" {...form.register(name)}/> : <Input id={name} {...form.register(name)} aria-invalid={!!form.formState.errors[name]}/>}<span className="field-error">{form.formState.errors[name]?.message}</span></div>)}<ErrorMessage error={create.error}/><Button disabled={create.isPending}>{create.isPending ? 'Создаём…' : 'Создать курс'}</Button></form></>;}
