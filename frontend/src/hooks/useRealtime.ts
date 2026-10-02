import { useCallback,useEffect,useState } from 'react';
import { Client } from '@stomp/stompjs';
import { getToken } from '../api/session';
import { examWebSocketSchema } from '../types/core';
import { notificationSchema } from '../api/services';
import { useQueryClient } from '@tanstack/react-query';
function useStomp(url:string|undefined,destination:string|null,receive:(body:string)=>void){
 const [connected,setConnected]=useState(false);
 useEffect(()=>{
  if(!url || !destination)return;
  const client=new Client({brokerURL:(()=>{const target=new URL(url,window.location.href);target.protocol=target.protocol==='https:' ? 'wss:' : target.protocol==='http:' ? 'ws:' : target.protocol;return target.href;})(),reconnectDelay:3000,heartbeatIncoming:10000,heartbeatOutgoing:10000,
   beforeConnect:()=>{client.connectHeaders={Authorization:`Bearer ${getToken() ?? ''}`};},
   onConnect:()=>{setConnected(true);client.subscribe(destination,message=>receive(message.body));},
   onWebSocketClose:()=>setConnected(false),onStompError:()=>setConnected(false)});
  client.activate();return()=>{setConnected(false);void client.deactivate();};
 },[url,destination,receive]);
 return connected;
}
export function useExamTimer(attemptId:number|null){
 const [frame,setFrame]=useState<ReturnType<typeof examWebSocketSchema.parse>|null>(null);
 const receive=useCallback((body:string)=>{try{const result=examWebSocketSchema.safeParse(JSON.parse(body));if(result.success)setFrame(result.data);}catch{}},[]);
 const connected=useStomp(import.meta.env.VITE_WS_URL,attemptId ? `/topic/exams/${attemptId}` : null,receive);
 return {frame:frame?.attemptId===attemptId ? frame : null,connected};
}
export function useNotificationRealtime(userId:number){
 const cache=useQueryClient();
 const receive=useCallback((body:string)=>{try{const result=notificationSchema.safeParse(JSON.parse(body));if(result.success && result.data.userId===userId)void cache.invalidateQueries({queryKey:['notifications',userId]});}catch{}},[cache,userId]);
 return useStomp(import.meta.env.VITE_NOTIFICATION_WS_URL,`/topic/notifications/${userId}`,receive);
}
