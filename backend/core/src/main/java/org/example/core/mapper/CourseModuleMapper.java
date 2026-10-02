package org.example.core.mapper;
import org.example.core.model.CourseModule;
import org.example.core.dto.response.CourseModuleResponse;
import org.springframework.stereotype.Component;
@Component
public class CourseModuleMapper {
 public CourseModuleResponse toDto(CourseModule entity) { return new CourseModuleResponse(entity.getId(), entity.getCourse().getId(), entity.getTitle(), entity.getOrderIndex(), entity.getCreatedAt()); }
}
