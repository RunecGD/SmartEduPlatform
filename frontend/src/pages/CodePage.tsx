import '../components/monaco';
import { useState } from 'react';
import Editor from '@monaco-editor/react';
import { Play, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { useExecuteCode } from '../hooks/useApi';
import { serviceUrls } from '../api/axios';
import { ErrorMessage } from '../components/Feedback';
import type { ExecutionStatus } from '../types/services';
const statusLabels: Record<ExecutionStatus,string> = { SUCCESS:'Выполнено', COMPILATION_ERROR:'Ошибка компиляции', RUNTIME_ERROR:'Ошибка выполнения', TIME_LIMIT_EXCEEDED:'Превышено время выполнения', INTERNAL_ERROR:'Ошибка сервиса' };
export function CodePage() {
  const [language, setLanguage] = useState(''); const [code, setCode] = useState(''); const [stdin, setStdin] = useState('');
  const execution = useExecuteCode();
  const valid = language.trim().length > 0 && code.trim().length > 0 && code.length <= 100000 && stdin.length <= 10000;
  const run = () => { execution.mutate({language:language.trim(),code,stdin},{onSuccess: result => { if(result.status === 'SUCCESS') toast.success('Код выполнен'); else toast.error(statusLabels[result.status]); }}); };
  return <><div className="page-heading"><div><div className="form-overline">ПРАКТИКА</div><h1>Редактор кода</h1><p>Проверяйте идеи и изучайте результат выполнения.</p></div></div>
    <div className="code-toolbar"><div className="language-field"><Label htmlFor="language">Язык</Label><Input id="language" value={language} onChange={e => setLanguage(e.target.value)} placeholder="Название языка" disabled={execution.isPending}/></div><Button onClick={run} disabled={!valid || execution.isPending || !serviceUrls.execution}>{execution.isPending ? <Loader2 className="animate-spin"/> : <Play/>}{execution.isPending ? 'Выполняется…' : 'Запустить'}</Button></div>
    <div className="editor-surface"><Editor height="370px" language={language.trim().toLowerCase() || 'plaintext'} value={code} onChange={value => setCode(value ?? '')} theme="vs-dark" loading={<p className="p-6">Загрузка редактора…</p>} options={{ariaLabel:'Исходный код',fontSize:15,minimap:{enabled:false},padding:{top:20},scrollBeyondLastLine:false,automaticLayout:true,readOnly:execution.isPending,tabSize:4}}/></div>
    <div className="code-bottom"><section className="code-pane"><h2><Label htmlFor="stdin">Входные данные · stdin</Label></h2><Textarea id="stdin" value={stdin} onChange={e => setStdin(e.target.value)} maxLength={10000} placeholder="Необязательные входные данные" disabled={execution.isPending}/></section><section className="code-pane" aria-live="polite" aria-busy={execution.isPending}><h2>Результат</h2>{execution.isPending ? <p className="code-help">Ожидаем ответ сервера…</p> : execution.data ? <><div className="run-status"><span>{statusLabels[execution.data.status]}</span>{execution.data.executionTimeMs !== null && <span>{execution.data.executionTimeMs} мс</span>}{execution.data.exitCode !== null && <span>Код выхода: {execution.data.exitCode}</span>}</div><div className="code-output">{execution.data.stdout || 'Программа не вывела текст.'}{execution.data.stderr && <><h3>stderr</h3>{execution.data.stderr}</>}</div></> : <p className="code-help">Результат появится после запуска кода.</p>}<ErrorMessage error={execution.error}/></section></div>
    <p className="code-help">Укажите название языка, поддерживаемого вашим сервером. Код — до 100 000 символов, входные данные — до 10 000.</p>{code.length > 100000 && <p role="alert" className="field-error">Код превышает допустимый размер.</p>}{!serviceUrls.execution && <p className="connection-note">Выполнение кода будет доступно после подключения сервиса.</p>}
  </>;
}
