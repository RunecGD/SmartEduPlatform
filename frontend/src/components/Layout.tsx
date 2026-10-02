import { Link, Outlet, useLocation } from 'react-router-dom';
import { BookOpen, Code2, LogOut, Bell, UserRound, ChartNoAxesColumnIncreasing, GraduationCap, Users, Settings } from 'lucide-react';
import { Sidebar, SidebarProvider, SidebarHeader, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupLabel, SidebarMenu, SidebarMenuItem, SidebarMenuButton, SidebarInset, SidebarTrigger, useSidebar } from '@/components/ui/sidebar';
import { Button } from '@/components/ui/button';
import { Brand } from './Brand';
import { ThemeToggle } from './ThemeToggle';
import { useAuth } from '../context/AuthContext';
function Navigation() {
 const {pathname}=useLocation();const {setOpenMobile}=useSidebar();const {signOut,user}=useAuth();
 const links=[{to:'/courses',label:'Каталог курсов',Icon:BookOpen},{to:'/code',label:'Практика кода',Icon:Code2}];
 if(user?.role==='STUDENT') links.push({to:'/student/progress',label:'Мой прогресс',Icon:ChartNoAxesColumnIncreasing});
 if(user?.role==='TEACHER') links.push({to:'/teacher/courses',label:'Мои курсы',Icon:GraduationCap});
 if(user?.role==='METHODIST') links.push({to:'/groups',label:'Мои группы',Icon:Users});
 if(user?.role==='ADMIN') links.push({to:'/admin',label:'Управление',Icon:Settings},{to:'/groups',label:'Учебные группы',Icon:Users});
 if(user)links.push({to:'/notifications',label:'Уведомления',Icon:Bell});
 links.push({to:'/profile',label:'Профиль',Icon:UserRound});
 return <Sidebar><SidebarHeader className="p-6"><Brand/></SidebarHeader><SidebarContent><SidebarGroup><SidebarGroupLabel className="text-sm mb-3">Учебное пространство</SidebarGroupLabel><SidebarMenu>{links.map(({to,label,Icon})=><SidebarMenuItem key={to}><SidebarMenuButton className="h-11" asChild isActive={pathname===to}><Link to={to} onClick={()=>setOpenMobile(false)}><Icon/><span>{label}</span></Link></SidebarMenuButton></SidebarMenuItem>)}</SidebarMenu></SidebarGroup></SidebarContent><SidebarFooter className="p-4">{user && <div className="user-label"><span className="avatar">{user.fullName.slice(0,1)}</span><div><strong>{user.fullName}</strong><small>{user.email}</small></div></div>}<Button variant="ghost" className="justify-start" onClick={signOut}><LogOut/>Выйти из аккаунта</Button></SidebarFooter></Sidebar>;
}
export function Layout(){return <SidebarProvider><Navigation/><SidebarInset className="min-w-0"><header className="workspace-header"><div><SidebarTrigger aria-label="Открыть меню"/><span>Учебное пространство</span></div><ThemeToggle/></header><div className="workspace-content"><Outlet/></div></SidebarInset></SidebarProvider>;}
