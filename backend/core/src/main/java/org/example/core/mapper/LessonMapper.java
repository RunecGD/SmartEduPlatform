package org.example.core.mapper;
import org.example.core.model.Lesson;
import org.example.core.dto.response.LessonResponse;
import org.springframework.stereotype.Component;
@Component
public class LessonMapper {
 public LessonResponse toDto(Lesson entity) { return new LessonResponse(entity.getId(), entity.getModule().getId(), entity.getType(), entity.getTitle(), entity.getContent(), entity.getOrderIndex(), entity.getCreatedAt()); }
}
