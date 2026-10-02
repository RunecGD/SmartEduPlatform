import { z } from 'zod';
export const loginSchema = z.object({ email: z.string().min(1, 'Введите email'), password: z.string().min(1, 'Введите пароль') });
export const loginFormSchema = loginSchema.extend({ fullName: z.string() });
export const registerSchema = z.object({ email: z.string().email('Введите корректный email').max(255), fullName: z.string().trim().min(1, 'Введите ФИО').max(100, 'Максимум 100 символов'), password: z.string().min(8, 'Не менее 8 символов').max(255, 'Максимум 255 символов') });
export type LoginValues = z.infer<typeof loginSchema>;
export type RegisterValues = z.infer<typeof registerSchema>;
