import {useEffect,useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {Button} from '@/components/ui/button';
import {Textarea} from '@/components/ui/textarea';
import {contentApi} from '../api/content';
import {examsApi} from '../api/exams';
import {useStartAttempt,useSubmitAttempt} from '../hooks/useApi';
import {useAuth} from '../context/AuthContext';
import {useExamTimer} from '../hooks/useRealtime';
import {PageHeading,useRouteId,InvalidId,Loading,QueryError} from '../components/Page';
import {ErrorMessage} from '../components/Feedback';
export function ExamPage(){const id=useRouteId('examId');return id ? <Exam id={id}/> : <InvalidId/>;}
function Exam({id}:{id:number}){
 const navigate=useNavigate(),{user}=useAuth();const [attemptId,setAttemptId]=useState<number|null>(null),[answers,setAnswers]=useState<Record<number,string>>({});
 const exam=useQuery({queryKey:['exams',id],queryFn:()=>contentApi.exam(id)});
 const start=useStartAttempt(),submit=useSubmitAttempt(attemptId ?? 0);
 const questions=useQuery({queryKey:['exams',id,'questions',attemptId],queryFn:()=>contentApi.questions(id),enabled:!!attemptId || user?.role==='TEACHER' || user?.role==='ADMIN'});
 const status=useQuery({queryKey:['attempts',attemptId],queryFn:()=>examsApi.getAttempt(attemptId!),enabled:!!attemptId,refetchInterval:5000});
 const timer=useExamTimer(attemptId);
 const finished=status.data && status.data.status!=='IN_PROGRESS';
 useEffect(()=>{if(finished && attemptId)navigate(`/exams/attempts/${attemptId}`,{replace:true});},[finished,attemptId,navigate]);
 if(exam.isLoading)return <Loading/>;if(exam.error)return <QueryError error={exam.error} retry={()=>void exam.refetch()}/>;if(!exam.data)return null;
 const active=!!attemptId && !finished && timer.frame?.status!=='EXPIRED';
 return <><PageHeading eyebrow="ЭКЗАМЕН" title={exam.data.title || 'Экзамен'} description={`Лимит: ${exam.data.timeLimitMinutes} минут`}/>{user?.role==='STUDENT' && !attemptId && <Button disabled={start.isPending} onClick={()=>start.mutate(id,{onSuccess:a=>setAttemptId(a.attemptId)})}>Начать или продолжить попытку</Button>}<ErrorMessage error={start.error || submit.error || questions.error || status.error}/>{attemptId && <div className="panel mb-5"><p role="status">{timer.connected && timer.frame ? `Осталось ${timer.frame.remainingSeconds} сек.` : 'Соединение с таймером восстанавливается. Статус проверяется на сервере.'}</p></div>}
 {questions.data?.map((q,index)=><section className="panel mb-4" key={q.id}><h2>Вопрос {index+1} · {q.maxScore} баллов</h2><p className="prose-text mb-4">{q.question}</p>{user?.role==='STUDENT' && <Textarea aria-label={`Ответ на вопрос ${index+1}`} value={answers[q.id] ?? ''} onChange={e=>setAnswers({...answers,[q.id]:e.target.value})} maxLength={10000} disabled={!active || submit.isPending}/>}</section>)}
 {user?.role==='STUDENT' && attemptId && <Button disabled={!active || submit.isPending || !questions.data?.length} onClick={()=>submit.mutate(questions.data?.map(q=>({questionId:q.id,answerText:answers[q.id] ?? ''})),{onSuccess:a=>navigate(`/exams/attempts/${a.attemptId}`)})}>{submit.isPending ? 'AI проверяет ответы…' : 'Отправить на проверку AI'}</Button>}</>;
}
