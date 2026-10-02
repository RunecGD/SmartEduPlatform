import {useState} from 'react';
import {Link} from 'react-router-dom';
import {useQuery,useMutation,useQueryClient} from '@tanstack/react-query';
import {Sparkles,FileText} from 'lucide-react';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {toast} from 'sonner';
import {contentApi} from '../api/content';
import {materialsApi} from '../api/materials';
import {managementApi} from '../api/management';
import {useCompleteLesson,useAsk,useCreateExam,useGenerateExam,useCourse} from '../hooks/useApi';
import {useAuth} from '../context/AuthContext';
import {PageHeading,Loading,QueryError,useRouteId,InvalidId,Back} from '../components/Page';
import {ErrorMessage} from '../components/Feedback';
export function LessonPage(){const id=useRouteId('lessonId'),courseId=useRouteId('courseId');return id && courseId ? <Lesson key={id} id={id} courseId={courseId}/> : <InvalidId/>;}
function Lesson({id,courseId}:{id:number;courseId:number}){
 const cache=useQueryClient();const {user}=useAuth();const course=useCourse(courseId);
 const lesson=useQuery({queryKey:['lessons',id],queryFn:()=>contentApi.lesson(id)});
 const materials=useQuery({queryKey:['materials',id],queryFn:()=>contentApi.materials(id)});
 const exams=useQuery({queryKey:['exams','lesson',id],queryFn:()=>contentApi.exams(id)});
 const aiStatus=useQuery({queryKey:['ai-status',id],queryFn:()=>managementApi.aiStatus(id),refetchInterval:query=>(query.state.data?.pendingCount || query.state.data?.retryingCount) ? 5000 : false});
 const complete=useCompleteLesson(),ask=useAsk(id),createExam=useCreateExam(id),generate=useGenerateExam();
 const upload=useMutation({mutationFn:(file:File)=>materialsApi.upload(id,file),onSuccess:()=>{void cache.invalidateQueries({queryKey:['materials',id]});void cache.invalidateQueries({queryKey:['ai-status',id]});toast.success('Материал загружен; подготовка для AI выполняется в фоне');}});
 const download=useMutation({mutationFn:materialsApi.download});
 const [question,setQuestion]=useState(''),[title,setTitle]=useState(''),[minutes,setMinutes]=useState(15),[count,setCount]=useState(5);
 const owner=user?.role==='ADMIN' || (user?.role==='TEACHER' && course.data?.teacherId===user.id);
 const canAsk=user?.role==='STUDENT' || owner;
 const ready=(aiStatus.data?.readyCount ?? 0)>0;
 if(lesson.isLoading)return <Loading/>;if(lesson.error)return <QueryError error={lesson.error} retry={()=>void lesson.refetch()}/>;if(!lesson.data)return null;
 return <><Back to={`/courses/${courseId}`}>К курсу</Back><PageHeading eyebrow={lesson.data.type} title={lesson.data.title}/>
 <section className="panel"><p className="prose-text whitespace-pre-wrap">{lesson.data.content || 'Содержание пока не добавлено.'}</p>{lesson.data.type==='CODE' && <Button asChild className="mt-4"><Link to="/code">Открыть редактор кода</Link></Button>}{user?.role==='STUDENT' && <Button className="mt-5" disabled={complete.isPending} onClick={()=>complete.mutate({lessonId:id},{onSuccess:()=>toast.success('Урок завершён')})}>Завершить урок</Button>}<ErrorMessage error={complete.error}/></section>
 <section className="panel mt-5"><h2><FileText size={20}/>Материалы</h2><ErrorMessage error={materials.error || download.error || upload.error}/>
 {materials.isLoading ? <p>Загружаем материалы…</p> : !materials.data?.length && <p className="text-muted-foreground">Материалы ещё не загружены.</p>}
 {materials.data?.map(m=><div key={m.id} className="action-row"><span>{m.fileName}</span><Button variant="outline" disabled={download.isPending} onClick={()=>download.mutate(m.id)}>Получить ссылку</Button></div>)}
 {download.data && <a className="underline" href={download.data.url} target="_blank" rel="noreferrer">Открыть материал</a>}
 {owner && <label className="block mt-4 text-sm">Загрузить материал · PDF, DOCX или TXT до 25 МБ<Input className="mt-2" type="file" accept=".pdf,.docx,.txt" disabled={upload.isPending} onChange={e=>{const file=e.target.files?.[0];if(file)upload.mutate(file);e.target.value='';}}/></label>}
 </section>
 {canAsk && <section className="panel mt-5"><h2><Sparkles size={20}/>AI-помощник по уроку</h2><p className="text-sm text-muted-foreground mb-4">Задайте вопрос по загруженным материалам. Ответ сопровождается списком источников.</p>
 <div role="status" className="ai-message mb-4">{aiStatus.isLoading ? 'Проверяем готовность материалов…' : aiStatus.error ? 'Не удалось проверить готовность материалов.' : ready ? `Готово материалов: ${aiStatus.data?.readyCount} из ${aiStatus.data?.materialCount}.` : owner ? 'Загрузите учебный материал и дождитесь его подготовки для AI.' : 'Преподаватель ещё не подготовил материалы для AI.'}
 {!!aiStatus.data?.pendingCount && <p>Подготавливаем файлов: {aiStatus.data.pendingCount}.</p>}{!!aiStatus.data?.retryingCount && <p>Подготовка {aiStatus.data.retryingCount} файлов задерживается. Сервер повторяет обработку.</p>}
 <Button className="ml-2" variant="ghost" size="sm" disabled={aiStatus.isFetching} onClick={()=>void aiStatus.refetch()}>Обновить статус</Button></div>
 <form className="space-y-3" onSubmit={e=>{e.preventDefault();ask.mutate({question:question.trim()});}}><Textarea aria-label="Вопрос AI-помощнику" placeholder="Например: объясни тему на простом примере" value={question} onChange={e=>setQuestion(e.target.value)} maxLength={10000} required disabled={ask.isPending}/><Button type="submit" disabled={ask.isPending || !ready || !question.trim()}><Sparkles size={16}/>{ask.isPending ? 'Готовим ответ…' : 'Спросить AI'}</Button></form><ErrorMessage error={ask.error}/>
 {ask.data && <div className="ai-message" aria-live="polite"><p className="prose-text whitespace-pre-wrap">{ask.data.answer}</p>{!!ask.data.sources?.length && <><h3 className="subheading">Источники</h3><ul className="list-disc pl-5 text-sm">{ask.data.sources.map(source=><li key={source}>{source}</li>)}</ul></>}</div>}
 </section>}
 <section className="panel mt-5"><h2>Экзамены</h2><ErrorMessage error={exams.error || createExam.error || generate.error}/>
 {owner && <><p className="text-sm text-muted-foreground mb-3">AI создаёт вопросы по материалам этого урока. После начала первой попытки заменить вопросы нельзя — создайте новый экзамен.</p><label className="flex items-center gap-3 text-sm mb-4">Вопросов для генерации<Input className="w-24" type="number" min={1} max={20} value={count} onChange={e=>setCount(Number(e.target.value))}/></label></>}
 {exams.isLoading ? <p>Загружаем экзамены…</p> : !exams.data?.length && <p className="text-muted-foreground">Экзаменов пока нет.</p>}
 {exams.data?.map(exam=><div key={exam.id} className="action-row flex-wrap gap-3"><Link className="text-primary underline" to={`/exams/${exam.id}`}>{exam.title}</Link><span>{exam.timeLimitMinutes} мин.</span>{owner && <Button variant="outline" disabled={generate.isPending || !ready || !Number.isInteger(count) || count<1 || count>20} onClick={()=>generate.mutate({examId:exam.id,count},{onSuccess:()=>{void cache.invalidateQueries({queryKey:['exams']});toast.success('Вопросы сгенерированы — откройте экзамен для просмотра');}})}><Sparkles size={16}/>{generate.isPending && generate.variables?.examId===exam.id ? 'Создаём вопросы…' : 'Создать вопросы с AI'}</Button>}</div>)}
 {owner && <form className="space-y-3 mt-4" onSubmit={e=>{e.preventDefault();createExam.mutate({title:title.trim(),timeLimitMinutes:minutes},{onSuccess:()=>{setTitle('');void cache.invalidateQueries({queryKey:['exams','lesson',id]});}});}}><Input aria-label="Название экзамена" value={title} onChange={e=>setTitle(e.target.value)} maxLength={255} placeholder="Название нового экзамена" required/><label className="block text-sm">Время экзамена, минут<Input className="mt-2" type="number" min={1} max={1440} value={minutes} onChange={e=>setMinutes(Number(e.target.value))} required/></label><Button type="submit" disabled={createExam.isPending || !title.trim()}>Создать экзамен</Button></form>}
 </section></>;
}
