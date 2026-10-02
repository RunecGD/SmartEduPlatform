package org.example.core.websocket;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.ExamWebSocketResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class ExamWebSocketService {
 private final JdbcTemplate jdbc;
 private final SimpMessagingTemplate messaging;
 @Transactional
 public void updateTimers() {
  var frames=jdbc.query("SELECT a.id,a.started_at,a.status,e.time_limit_minutes FROM exam_attempts a JOIN exams e ON e.id=a.exam_id WHERE a.status='IN_PROGRESS' FOR UPDATE OF a SKIP LOCKED",(rs,n)-> {
   long id=rs.getLong("id");Instant started=rs.getTimestamp("started_at").toInstant();
   Instant deadline=started.plusSeconds(rs.getInt("time_limit_minutes")*60L);Instant now=Instant.now();
   long remaining=Math.max(0,(Duration.between(now,deadline).toMillis()+999)/1000);
   String status=now.isBefore(deadline) ? "IN_PROGRESS" : "EXPIRED";
   return new ExamWebSocketResponse(id,status,remaining,started,deadline);
  });
  for (var frame:frames) if (frame.status().equals("EXPIRED")) jdbc.update("UPDATE exam_attempts SET status='EXPIRED',finished_at=? WHERE id=? AND status='IN_PROGRESS'",java.sql.Timestamp.from(Instant.now()),frame.attemptId());
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
   @Override public void afterCommit() { for (var frame:frames) messaging.convertAndSend("/topic/exams/"+frame.attemptId(),frame); }
  });
 }
}
