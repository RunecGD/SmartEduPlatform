import { userSchema, type UserResponse } from '../types/core';
const KEY = 'smartedu.token';
const USER_KEY = 'smartedu.identity';
export const SESSION_EVENT = 'smartedu:session';
export const getToken = () => sessionStorage.getItem(KEY);
export function getUser(): UserResponse | null {
  try { const record = JSON.parse(sessionStorage.getItem(USER_KEY) ?? 'null'); if (!record || record.token !== getToken()) return null; const result = userSchema.safeParse(record.user); return result.success ? result.data : null; } catch { return null; }
}
export function setToken(token: string | null, user: UserResponse | null = null) {
  sessionStorage.removeItem(USER_KEY);
  if (token) { sessionStorage.setItem(KEY, token); if (user) sessionStorage.setItem(USER_KEY, JSON.stringify({token,user})); }
  else sessionStorage.removeItem(KEY);
  window.dispatchEvent(new Event(SESSION_EVENT));
}
