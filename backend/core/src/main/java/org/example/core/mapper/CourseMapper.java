package org.example.core.mapper;
import org.example.core.model.Course;
import org.example.core.dto.response.CourseResponse;
import org.springframework.stereotype.Component;
@Component
public class CourseMapper {
 public CourseResponse toDto(Course entity) { return new CourseResponse(entity.getId(), entity.getTeacher().getId(), entity.getTitle(), entity.getCategory(), entity.getDescription(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt()); }
}
