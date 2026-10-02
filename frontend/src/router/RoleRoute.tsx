import { Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Unavailable } from '../components/Feedback';
import type { Role } from '../types/core';
export function RoleRoute({roles}: {roles: Role[]}) {
  const {user} = useAuth();
  if(!user) return <Unavailable title="Профиль недоступен" message="Сервер не передал профиль при входе. Доступ к этому разделу пока нельзя определить."/>;
  if(!roles.includes(user.role) || user.isEnabled === false) return <Unavailable title="Нет доступа" message="Этот раздел недоступен для вашей учётной записи."/>;
  return <Outlet/>;
}
