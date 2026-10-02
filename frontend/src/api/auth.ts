import { z } from 'zod';
import { userSchema } from '../types/core';
import { core } from './axios';
import type { LoginRequest, UserRequest } from '../types/requests';
const loginResponse = z.object({ token: z.string().min(1) });
const registerResponse = loginResponse.extend({ user: userSchema });
export const authApi = {
  me: async (token?: string, signal?: AbortSignal) => userSchema.parse(await core({ url: '/api/v1/users/me', signal, ...(token ? {headers:{Authorization:`Bearer ${token}`}} : {}) })),
  login: async (data: LoginRequest) => { const result=loginResponse.parse(await core({ method: 'POST', url: '/api/v1/auth/login', data })); return {...result,user:await authApi.me(result.token)}; },
  register: async (data: UserRequest) => registerResponse.parse(await core({ method: 'POST', url: '/api/v1/auth/register', data })),
};
