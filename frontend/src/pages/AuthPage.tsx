import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { BookOpen, Code2, Eye, EyeOff, Loader2, ShieldCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { loginFormSchema, registerSchema, type RegisterValues } from '../schemas/auth';
import { useLogin, useRegister } from '../hooks/useApi';
import { useAuth } from '../context/AuthContext';
import { serviceUrls } from '../api/axios';
import { Brand } from '../components/Brand';
import { ThemeToggle } from '../components/ThemeToggle';
import { ErrorMessage } from '../components/Feedback';
export function AuthPage({register = false}: {register?: boolean}) {
  const [visible, setVisible] = useState(false);
  const auth = useAuth(); const navigate = useNavigate();
  const login = useLogin(); const registration = useRegister();
  const form = useForm<RegisterValues>({ resolver: zodResolver(register ? registerSchema : loginFormSchema), defaultValues: {email:'', password:'', fullName:''} });
  const pending = login.isPending || registration.isPending;
  const submit = form.handleSubmit(async values => {
    try { if(register) { const result = await registration.mutateAsync(values); auth.signIn(result.token, result.user); } else { const result = await login.mutateAsync({email:values.email,password:values.password}); auth.signIn(result.token,result.user); } navigate('/courses', {replace:true}); } catch { /* Rendered below; never expose raw server details. */ }
  });
  if (auth.token) return <Navigate to="/courses" replace/>;
  return <div className="auth-page">
    <aside className="auth-story"><Brand/><div className="story-main"><div className="eyebrow"><span/> ПРОСТРАНСТВО ДЛЯ РОСТА</div><h1>Знания.<br/>Практика.<br/><span>Ваш следующий шаг.</span></h1><p>Учитесь в своём темпе.<br/>Применяйте знания и двигайтесь дальше.</p><div className="story-features"><div><BookOpen/><span>От первого урока до результата</span></div><div><Code2/><span>Практика в одном пространстве</span></div></div></div><div className="story-footer"><span>SmartEdu Platform</span><span>Учиться. Создавать. Расти.</span></div></aside>
    <main className="auth-main"><header className="auth-top"><span className="mobile-brand"><Brand/></span><span className="top-caption">ВАШЕ ОБУЧЕНИЕ НАЧИНАЕТСЯ ЗДЕСЬ</span><ThemeToggle/></header>
      <div className="auth-form-wrap"><div className="form-overline">{register ? 'НОВЫЙ АККАУНТ' : 'С ВОЗВРАЩЕНИЕМ'}</div><h2>{register ? 'Начните свой путь' : 'Войдите в SmartEdu'}</h2><p className="form-intro">{register ? 'Создайте аккаунт для доступа к обучению.' : 'Продолжите обучение с того места, где остановились.'}</p>
      <div className="auth-tabs"><Link className={!register ? 'active' : ''} to="/login">Вход</Link><Link className={register ? 'active' : ''} to="/register">Регистрация</Link></div>
      <form onSubmit={submit} noValidate>
        {register && <div className="field"><Label htmlFor="fullName">ФИО</Label><Input id="fullName" autoComplete="name" placeholder="Иван Иванов" aria-invalid={!!form.formState.errors.fullName} aria-describedby="name-error" {...form.register('fullName')}/><span id="name-error" className="field-error">{form.formState.errors.fullName?.message}</span></div>}
        <div className="field"><Label htmlFor="email">Email</Label><Input id="email" type="email" autoComplete="email" placeholder="you@example.com" aria-invalid={!!form.formState.errors.email} aria-describedby="email-error" {...form.register('email')}/><span id="email-error" className="field-error">{form.formState.errors.email?.message}</span></div>
        <div className="field"><Label htmlFor="password">Пароль</Label><div className="password-wrap"><Input id="password" type={visible ? 'text' : 'password'} autoComplete={register ? 'new-password' : 'current-password'} placeholder={register ? 'Не менее 8 символов' : 'Введите пароль'} aria-invalid={!!form.formState.errors.password} aria-describedby="password-error" {...form.register('password')}/><Button type="button" variant="ghost" size="icon" aria-label={visible ? 'Скрыть пароль' : 'Показать пароль'} onClick={() => setVisible(!visible)}>{visible ? <EyeOff/> : <Eye/>}</Button></div><span id="password-error" className="field-error">{form.formState.errors.password?.message}</span></div>
        <ErrorMessage error={login.error || registration.error}/>
        {!serviceUrls.core && <p className="connection-note" role="status">Вход будет доступен после подключения платформы.</p>}
        <Button className="submit-button" type="submit" disabled={pending || !serviceUrls.core}>{pending && <Loader2 className="animate-spin"/>}{pending ? 'Подождите…' : register ? 'Создать аккаунт' : 'Войти в аккаунт'}</Button>
      </form><p className="auth-switch">{register ? 'Уже учитесь с нами?' : 'Ещё нет аккаунта?'} <Link to={register ? '/login' : '/register'}>{register ? 'Войти' : 'Зарегистрироваться'}</Link></p>
      <div className="secure-note"><ShieldCheck size={16}/><span>Ваше личное учебное пространство</span></div></div>
      <footer className="auth-footer"><span>SmartEdu © {new Date().getFullYear()}</span><span>Образование в вашем ритме</span></footer>
    </main></div>;
}
