package org.example.core.mapper;

import org.example.core.dto.response.CourseResponse;
import org.example.core.model.Course;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    CourseResponse toDto(Course course);

    default Page<CourseResponse> toDtoPage(Page<Course> employeesPage) {
        return employeesPage.map(this::toDto);
    }
}
