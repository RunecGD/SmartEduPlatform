import axios, { type AxiosRequestConfig } from 'axios';
import { getToken, setToken } from './session';
export const serviceUrls = {
  core: import.meta.env.VITE_API_URL as string | undefined,
  notifications: import.meta.env.VITE_NOTIFICATION_API_URL as string | undefined,
  execution: import.meta.env.VITE_CODE_API_URL as string | undefined,
};
export type Service = keyof typeof serviceUrls;
export class ApiError extends Error {
  constructor(message: string, public readonly status?: number) { super(message); this.name = 'ApiError'; }
}
export const api = axios.create({ baseURL: serviceUrls.core, timeout: 20000 });
api.interceptors.request.use(config => {
  const token = getToken();
  if (token && !config.headers.Authorization) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
api.interceptors.response.use(response => response, (error: unknown) => {
  if (!axios.isAxiosError(error)) return Promise.reject(new ApiError('Не удалось выполнить запрос.'));
  const status = error.response?.status;
  const isAuth = /\/auth\/(login|register)$/.test(error.config?.url ?? '');
  if (status === 401 && !isAuth) setToken(null);
  const message = status === 401 ? (isAuth ? 'Проверьте email и пароль.' : 'Сессия завершена. Войдите снова.')
    : status === 403 ? 'У вас нет доступа к этому действию.'
    : status === 404 ? 'Запрашиваемые данные не найдены.'
    : status === 409 ? 'Действие конфликтует с текущим состоянием данных.'
    : status === 400 || status === 422 ? 'Проверьте заполненные поля.'
    : status === 429 ? 'Слишком много запросов. Попробуйте позже.'
    : !error.response ? 'Не удалось подключиться к серверу. Попробуйте позже.'
    : 'Сервис временно недоступен. Попробуйте позже.';
  return Promise.reject(new ApiError(message, status));
});
export async function request<T>(service: Service, config: AxiosRequestConfig): Promise<T> {
  const baseURL = serviceUrls[service];
  if (!baseURL) throw new ApiError('Подключение к сервису ещё не настроено.');
  return (await api.request<T>({ ...config, baseURL })).data;
}
export const core = <T = unknown>(config: AxiosRequestConfig) => request<T>('core', config);
export function id(value: number | string) { return encodeURIComponent(String(value)); }
