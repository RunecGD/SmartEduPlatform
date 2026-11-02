import { Bell, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useNotifications, useReadNotification } from '../hooks/useApi';
import { ErrorMessage, Unavailable } from './Feedback';
import { Link } from 'react-router-dom';
/** Mount only with userId resolved from the authenticated backend identity contract. */
export function NotificationList({userId}: {userId:number}) {
  const query = useNotifications(userId); const read = useReadNotification(userId);
  if(query.isLoading) return <Skeleton className="h-40 w-full"/>;
  if(query.error) return <ErrorMessage error={query.error}/>;
  if(!query.data?.length) return <Unavailable title="Новых уведомлений нет" message="Здесь появятся события ваших курсов и обсуждений."/>;
  return <section><h2 className="flex items-center gap-2 mb-4"><Bell size={18}/>Уведомления <span>({query.data.filter(n => !n.read).length} непрочитанных)</span></h2><ErrorMessage error={read.error}/><ul>{query.data.map(notification => <li key={notification.id} className="border-b py-4 flex flex-wrap items-center gap-4"><div className="flex-1 min-w-40"><p>{notification.message}</p>{notification.type==='EXAM_SUBMITTED' && notification.targetUrl && /^\/exams\/attempts\/[1-9]\d*$/.test(notification.targetUrl) && <Link className="mt-2 inline-block text-primary underline" to={notification.targetUrl} onClick={()=>{if(!notification.read)read.mutate(notification.id);}}>Открыть работу для проверки</Link>}</div>{notification.read ? <span className="text-muted-foreground text-sm">Прочитано</span> : <Button variant="outline" size="sm" disabled={read.isPending} onClick={() => read.mutate(notification.id)}><Check/>Отметить прочитанным</Button>}</li>)}</ul></section>;
}
