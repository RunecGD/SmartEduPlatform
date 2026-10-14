import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { useAttempt } from '../hooks/useApi';
import { examsApi, type ExamGradeRequest } from '../api/exams';
import type { ExamAttemptResponse } from '../types/core';
import { ErrorMessage } from '../components/Feedback';
import { PageHeading, Loading, QueryError, useRouteId, InvalidId, numberLabel, statusLabel, dateLabel } from '../components/Page';

export function AttemptPage() {
 const id = useRouteId('attemptId');
 return id ? <Attempt key={id} id={id}/> : <InvalidId/>;
}
function Attempt({id}: {id:number}) {
 const query = useAttempt(id);
 const client = useQueryClient();
 const [draft, setDraft] = useState<ExamGradeRequest[] | null>(null);
 const [validation, setValidation] = useState('');
 const completed = (data: ExamAttemptResponse) => {
  client.setQueryData(['attempts', id], data);
  void client.invalidateQueries();
  setDraft(null);
  setValidation('');
 };
 const ai = useMutation({mutationFn: () => examsApi.gradeWithAi(id), onSuccess: completed});
 const manual = useMutation({mutationFn: (grades: ExamGradeRequest[]) => examsApi.gradeManually(id, grades), onSuccess: completed});
 const busy = ai.isPending || manual.isPending;
 if (query.isLoading) return <Loading/>;
 if (query.error) return <QueryError error={query.error} retry={() => void query.refetch()}/>;
 if (!query.data) return null;
 const data = query.data;
 const reviewable = data.canReview && (data.status === 'SUBMITTED' || data.status === 'FINISHED') && !!data.answers?.length;
 const edit = () => {
  ai.reset(); manual.reset(); setValidation('');
  setDraft((data.answers ?? []).map(a => ({questionId: a.questionId, score: a.score ?? 0, feedback: a.feedback ?? ''})));
 };
 const save = () => {
  if (!draft) return;
  if (draft.some(g => {
   const max = data.answers?.find(a => a.questionId === g.questionId)?.maxScore;
   return !Number.isInteger(g.score) || g.score < 0 || max == null || g.score > max;
  })) { setValidation('Укажи целые баллы от 0 до максимума каждого вопроса.'); return; }
  setValidation(''); manual.mutate(draft);
 };
 return <>
  <PageHeading eyebrow={`ЭКЗАМЕН #${data.examId}`} title={`Работа #${data.attemptId}`} description={statusLabel(data.status)}>
   <Button variant="outline" disabled={query.isFetching || busy || !!draft} onClick={() => void query.refetch()}><RefreshCw/>Обновить</Button>
  </PageHeading>
  {data.status === 'SUBMITTED' && <p className="panel mb-5" role="status">Ответы отправлены. Работа ожидает проверки преподавателем.</p>}
  {data.status === 'EXPIRED' && <p className="panel mb-5">Время попытки истекло. Повторное прохождение этого экзамена недоступно.</p>}
  <div className="stats-grid">
   <div className="stat-card"><span>Итоговый балл</span><strong>{numberLabel(data.totalScore)}</strong></div>
   <div className="stat-card"><span>Начало</span><strong className="date-stat">{dateLabel(data.startedAt)}</strong></div>
   <div className="stat-card"><span>Завершение попытки</span><strong className="date-stat">{dateLabel(data.finishedAt)}</strong></div>
  </div>
  {reviewable && <section className="panel mb-5">
   <h2>Проверка работы</h2>
   <p className="mb-4">Выберите ручную проверку или помощь ИИ. После проверки оценку и комментарии можно изменить вручную.</p>
   <div className="flex flex-wrap gap-3">
    {data.status === 'SUBMITTED' && <Button disabled={busy || !!draft} onClick={() => { manual.reset(); ai.mutate(); }}>{ai.isPending ? 'ИИ проверяет…' : 'Проверить с ИИ'}</Button>}
    {!draft && <Button variant="outline" disabled={busy} onClick={edit}>{data.status === 'FINISHED' ? 'Изменить оценку' : 'Проверить вручную'}</Button>}
    {draft && <><Button disabled={busy} onClick={save}>{manual.isPending ? 'Сохранение…' : 'Сохранить оценку'}</Button><Button variant="outline" disabled={busy} onClick={() => {setDraft(null); setValidation(''); manual.reset();}}>Отмена</Button></>}
   </div>
   <ErrorMessage error={ai.error || manual.error}/>
   {validation && <p role="alert" className="mt-3 text-destructive">{validation}</p>}
  </section>}
  <div className="space-y-5">{data.answers?.map((answer,index) => {
   const grade = draft?.find(g => g.questionId === answer.questionId);
   const change = (patch: Partial<ExamGradeRequest>) => setDraft(current => current?.map(g => g.questionId === answer.questionId ? {...g,...patch} : g) ?? null);
   return <section className="panel" key={answer.questionId}>
    <div className="section-heading"><h2>Вопрос {index+1}</h2><span>{numberLabel(answer.score)} / {numberLabel(answer.maxScore)} баллов</span></div>
    <p className="prose-text">{answer.question || 'Текст вопроса недоступен.'}</p>
    <h3 className="subheading">Ответ ученика</h3><p className="answer-text">{answer.answer || 'Ответ не указан.'}</p>
    {grade ? <div className="space-y-3 mt-4">
     <label className="block">Баллы (0–{answer.maxScore})<Input type="number" min={0} max={answer.maxScore ?? 0} step={1} disabled={busy} value={Number.isNaN(grade.score) ? '' : grade.score} onChange={e => change({score: e.target.value === '' ? Number.NaN : Number(e.target.value)})}/></label>
     <label className="block">Комментарий<Textarea value={grade.feedback} maxLength={10000} disabled={busy} onChange={e => change({feedback:e.target.value})}/></label>
    </div> : answer.feedback && <><h3 className="subheading">Комментарий к оценке</h3><p className="prose-text">{answer.feedback}</p></>}
   </section>;
  })}</div>
  {!data.answers?.length && <p className="text-muted-foreground">Сохранённых ответов пока нет.</p>}
 </>;
}

