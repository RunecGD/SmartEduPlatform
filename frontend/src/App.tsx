import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes, Link } from 'react-router-dom';
import { AdminPage } from './pages/AdminPage';
import { GroupsPage, ResultsPage } from './pages/ResultsPages';
import { AuthPage } from './pages/AuthPage';
import { CoursesPage } from './pages/CoursesPage';
import { CoursePage } from './pages/CoursePage';
import { ForumPage, TopicPage } from './pages/ForumPages';
import { ProgressPage } from './pages/ProgressPage';
import { AttemptPage } from './pages/AttemptPage';
import {LessonPage} from './pages/LessonPage';
import {ExamPage} from './pages/ExamPage';
import { CreateCoursePage } from './pages/CreateCoursePage';
import { ProfilePage, NotificationsPage } from './pages/AccountPages';
import { ProtectedRoute } from './router/ProtectedRoute';
import { RoleRoute } from './router/RoleRoute';
import { Layout } from './components/Layout';
import { Unavailable } from './components/Feedback';
const CodePage = lazy(() => import('./pages/CodePage').then(module => ({ default: module.CodePage })));
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage').then(module => ({default:module.AnalyticsPage})));
export default function App() {
 return <Suspense fallback={<p className="p-8">Загрузка…</p>}><Routes><Route path="/" element={<Navigate to="/login" replace/>}/><Route path="/login" element={<AuthPage key="login"/>}/><Route path="/register" element={<AuthPage key="register" register/>}/><Route element={<ProtectedRoute/>}><Route element={<Layout/>}>
 <Route path="/courses" element={<CoursesPage/>}/><Route path="/courses/:courseId" element={<CoursePage/>}/><Route path="/courses/:courseId/lessons/:lessonId" element={<LessonPage/>}/><Route path="/code" element={<CodePage/>}/><Route path="/profile" element={<ProfilePage/>}/><Route path="/notifications" element={<NotificationsPage/>}/>
 <Route element={<RoleRoute roles={['STUDENT','TEACHER','ADMIN']}/>}><Route path="/courses/:courseId/forum" element={<ForumPage/>}/><Route path="/courses/:courseId/forum/:topicId" element={<TopicPage/>}/><Route path="/topics/:topicId" element={<TopicPage/>}/><Route path="/exams/:examId" element={<ExamPage/>}/></Route>
 <Route element={<RoleRoute roles={['STUDENT']}/>}><Route path="/student" element={<ProgressPage/>}/><Route path="/student/progress" element={<ProgressPage/>}/></Route>
 <Route element={<RoleRoute roles={['TEACHER']}/>}><Route path="/teacher" element={<CoursesPage teacher/>}/><Route path="/teacher/courses" element={<CoursesPage teacher/>}/><Route path="/teacher/courses/create" element={<CreateCoursePage/>}/></Route>
 <Route element={<RoleRoute roles={['STUDENT','TEACHER','METHODIST','ADMIN']}/>}><Route path="/exams/attempts/:attemptId" element={<AttemptPage/>}/></Route>
 <Route element={<RoleRoute roles={['TEACHER','ADMIN']}/>}><Route path="/reports/courses/:courseId" element={<ResultsPage/>}/><Route path="/teacher/analytics/:courseId" element={<AnalyticsPage/>}/></Route>
 <Route element={<RoleRoute roles={['METHODIST','ADMIN']}/>}><Route path="/groups" element={<GroupsPage/>}/><Route path="/groups/:groupId/results" element={<ResultsPage group/>}/></Route>
 <Route element={<RoleRoute roles={['ADMIN']}/>}><Route path="/admin" element={<AdminPage/>}/></Route>
 </Route></Route><Route path="*" element={<main className="not-found"><Unavailable title="Страница не найдена" message="Проверьте адрес или вернитесь на главную."/><Link to="/">На главную</Link></main>}/></Routes></Suspense>;
}
