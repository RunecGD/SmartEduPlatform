import type { UserResponse } from '../types/core';
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getToken, getUser, setToken, SESSION_EVENT } from '../api/session';
import { authApi } from '../api/auth';
interface AuthState { token: string | null; user: UserResponse | null; signIn: (token: string, user?: UserResponse | null) => void; signOut: () => void }
const AuthContext = createContext<AuthState | null>(null);
export function AuthProvider({ children }: {children: ReactNode}) {
  const [token, updateToken] = useState(getToken);
  const [user, updateUser] = useState(getUser);
  const client = useQueryClient();
  useEffect(() => { if(!token) return; const controller=new AbortController(); void authApi.me(token,controller.signal).then(updateUser).catch(()=>{}); return ()=>controller.abort(); },[token]);
  useEffect(() => {
    const sync = () => { client.clear(); updateToken(getToken()); updateUser(getUser()); };
    window.addEventListener(SESSION_EVENT, sync);
    return () => window.removeEventListener(SESSION_EVENT, sync);
  }, [client]);
  return <AuthContext.Provider value={{token, user, signIn: setToken, signOut: () => setToken(null)}}>{children}</AuthContext.Provider>;
}
export function useAuth() { const auth = useContext(AuthContext); if (!auth) throw new Error('AuthProvider missing'); return auth; }
