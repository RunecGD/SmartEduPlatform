import { AlertCircle, BookOpen } from 'lucide-react';
import { Empty, EmptyHeader, EmptyMedia, EmptyTitle, EmptyDescription } from '@/components/ui/empty';
import { ApiError } from '../api/axios';
export function ErrorMessage({error}: {error: unknown}) {
  if (!error) return null;
  return <div role="alert" className="error-box"><AlertCircle size={18}/><span>{error instanceof ApiError ? error.message : 'Не удалось обработать ответ сервиса. Попробуйте позже.'}</span></div>;
}
export function Unavailable({title = 'Раздел пока недоступен', message = 'Мы готовим ваше учебное пространство. Пожалуйста, зайдите позже.'}: {title?: string; message?: string}) {
  return <Empty className="empty-panel"><EmptyHeader><EmptyMedia variant="icon"><BookOpen/></EmptyMedia><EmptyTitle>{title}</EmptyTitle><EmptyDescription>{message}</EmptyDescription></EmptyHeader></Empty>;
}
