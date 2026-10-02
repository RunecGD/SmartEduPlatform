import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage, Unavailable } from './Feedback';
export function PageHeading({eyebrow,title,description,children}: {eyebrow?: string;title:string;description?:string;children?:ReactNode}) { return <div className="page-heading"><div>{eyebrow && <div className="form-overline">{eyebrow}</div>}<h1>{title}</h1>{description && <p>{description}</p>}</div>{children}</div>; }
export function Loading() { return <div aria-label="Загрузка" className="space-y-4"><Skeleton className="h-12 w-2/3"/><Skeleton className="h-64 w-full"/></div>; }
export function QueryError({error,retry}: {error:unknown;retry:()=>void}) {return <><ErrorMessage error={error}/><button className="text-primary underline" onClick={retry}>Попробовать снова</button></>;}
export function Back({to,children}: {to:string;children:ReactNode}) {return <Link className="back-link" to={to}>{children}</Link>;}
export function useRouteId(key:string) {const value = Number(useParams()[key]);return Number.isSafeInteger(value) && value > 0 ? value : 0;}
export function InvalidId() {return <Unavailable title="Некорректный адрес" message="Проверьте ссылку на страницу."/>;}
export const dateLabel = (value: string | null) => {if(!value)return '—';const date = new Date(value);return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat('ru-RU',{dateStyle:'medium'}).format(date);};
export const numberLabel = (value: number | null | undefined, suffix = '') => value == null ? '—' : `${new Intl.NumberFormat('ru-RU',{maximumFractionDigits:1}).format(value)}${suffix}`;
export const statusLabel = (status:string) => ({DRAFT:'Черновик',PUBLISHED:'Опубликован',ARCHIVED:'В архиве',IN_PROGRESS:'В процессе',FINISHED:'Завершено',EXPIRED:'Время истекло'}[status] ?? status);
