import { useEffect, useState } from 'react';
import { Moon, Sun } from 'lucide-react';
import { Button } from '@/components/ui/button';
export function ThemeToggle() {
  const [dark, setDark] = useState(() => localStorage.getItem('smartedu.theme') === 'dark');
  useEffect(() => { document.documentElement.classList.toggle('dark', dark); localStorage.setItem('smartedu.theme', dark ? 'dark' : 'light'); }, [dark]);
  return <Button variant="ghost" size="icon" aria-label={dark ? 'Светлая тема' : 'Тёмная тема'} onClick={() => setDark(!dark)}>{dark ? <Sun/> : <Moon/>}</Button>;
}
