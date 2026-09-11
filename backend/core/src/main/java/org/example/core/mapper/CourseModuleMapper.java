package org.example.core.mapper;

import org.example.core.dto.response.CourseModuleResponse;
import org.example.core.model.CourseModule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseModuleMapper {
    CourseModuleResponse toDto(CourseModule module);

}
