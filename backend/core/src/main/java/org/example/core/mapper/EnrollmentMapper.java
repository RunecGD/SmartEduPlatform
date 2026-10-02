package org.example.core.mapper;
import org.example.core.model.Enrollment;
import org.example.core.dto.response.EnrollmentResponse;
import org.springframework.stereotype.Component;
@Component
public class EnrollmentMapper {
 public EnrollmentResponse toDto(Enrollment entity) { return new EnrollmentResponse(entity.getId(), entity.getUser().getId(), entity.getCourse().getId(), entity.getProgressPct(), entity.getEnrolledAt()); }
}
