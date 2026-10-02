package com.example.notificationservice.service;
import com.example.notificationservice.dto.*;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketService webSocketService;
    private final MongoTemplate mongoTemplate;

    private void create(String id,Long userId,NotificationType type,String message) {
        if (userId==null || userId<=0) throw new IllegalArgumentException("Некорректный userId события");
        Query query=Query.query(Criteria.where("_id").is(id));
        Update update=new Update().setOnInsert("userId",userId).setOnInsert("type",type)
                .setOnInsert("message",message).setOnInsert("read",false)
                .setOnInsert("createdAt",LocalDateTime.now(ZoneOffset.UTC));
        try {
            Notification previous=mongoTemplate.findAndModify(query,update,
                    FindAndModifyOptions.options().upsert(true).returnNew(false),Notification.class);
            if (previous!=null) return;
        } catch (DuplicateKeyException duplicate) { return; }
        Notification saved=mongoTemplate.findById(id,Notification.class);
        if (saved!=null) {
            try { webSocketService.sendNotification(NotificationResponse.from(saved)); }
            catch (RuntimeException ex) { log.warn("Realtime delivery failed; notification retained"); }
        }
    }
    public void createExamPassedNotification(ExamPassedEvent event) {
        if (event.attemptId()==null || event.examId()==null || event.score()==null || event.score()<0) {
            throw new IllegalArgumentException("Некорректное exam.passed событие");
        }
        create("exam:"+event.attemptId(),event.userId(),NotificationType.EXAM_PASSED,
                "Вы сдали экзамен. Результат: "+event.score()+" баллов.");
    }
    public void createUserEnrolledNotification(UserEnrolledEvent event) {
        if (event.courseId()==null) throw new IllegalArgumentException("Нет courseId");
        create("enrolled:"+event.userId()+":"+event.courseId(),event.userId(),NotificationType.USER_ENROLLED,
                "Вы успешно записались на курс.");
    }
    public void createForumReplyNotification(ForumReplyEvent event) {
        if (event.topicId()==null || event.postId()==null) throw new IllegalArgumentException("Нет topicId/postId");
        create("reply:"+event.userId()+":"+event.postId(),event.userId(),NotificationType.FORUM_REPLY,
                "В вашей теме форума появился новый ответ.");
    }
    public void createForumAnsweredNotification(ForumAnsweredEvent event) {
        if (event.questionId()==null || event.answerId()==null) throw new IllegalArgumentException("Нет questionId/answerId");
        create("answered:"+event.userId()+":"+event.answerId(),event.userId(),NotificationType.FORUM_REPLY,
                "На ваш вопрос получен ответ.");
    }
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList();
    }
    public NotificationResponse markAsRead(String id,Long userId) {
        Query query=Query.query(Criteria.where("_id").is(id).and("userId").is(userId));
        Notification updated=mongoTemplate.findAndModify(query,new Update().set("read",true),
                FindAndModifyOptions.options().returnNew(true),Notification.class);
        if (updated==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Уведомление не найдено");
        return NotificationResponse.from(updated);
    }
}
