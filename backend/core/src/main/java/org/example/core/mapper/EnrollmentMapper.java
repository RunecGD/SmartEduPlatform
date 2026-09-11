package org.example.core.mapper;

import org.example.core.dto.response.EnrollmentResponse;
import org.example.core.model.Enrollment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    EnrollmentResponse toDto(Enrollment enrollment);

}
