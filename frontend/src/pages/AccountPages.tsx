import { Button } from '@/components/ui/button';
import { useAuth } from '../context/AuthContext';
import { NotificationList } from '../components/NotificationList';
import { PageHeading, dateLabel } from '../components/Page';
import { Unavailable } from '../components/Feedback';
const roleNames={STUDENT:'Студент',TEACHER:'Преподаватель',METHODIST:'Куратор',ADMIN:'Администратор'};
export function ProfilePage(){const {user,signOut}=useAuth();return <><PageHeading eyebrow="АККАУНТ" title="Мой профиль"/><section className="panel max-w-2xl">{user ? <><h2>{user.fullName}</h2><dl className="profile-details"><dt>Email</dt><dd>{user.email}</dd><dt>Роль</dt><dd>{roleNames[user.role]}</dd><dt>Регистрация</dt><dd>{dateLabel(user.createdAt)}</dd><dt>Обновление профиля</dt><dd>{dateLabel(user.updatedAt)}</dd><dt>Статус</dt><dd>{user.isEnabled == null ? 'Не указан' : user.isEnabled ? 'Активен' : 'Отключён'}</dd></dl></> : <Unavailable title="Профиль недоступен" message="Вход выполнен, но сервер не передал данные вашего профиля."/>}<Button variant="outline" onClick={signOut}>Выйти из аккаунта</Button></section></>;}
export function NotificationsPage(){const {user}=useAuth();return <><PageHeading eyebrow="СОБЫТИЯ" title="Уведомления" description="Новости ваших курсов и обсуждений."/>{user ? <NotificationList userId={user.id}/> : <Unavailable title="Уведомления пока недоступны" message="Чтобы загрузить уведомления, нужен профиль текущего пользователя."/>}</>;}
