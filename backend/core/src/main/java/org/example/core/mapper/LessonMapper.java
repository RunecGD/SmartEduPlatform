package org.example.core.mapper;

import org.example.core.dto.response.LessonResponse;
import org.example.core.model.Lesson;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LessonMapper {
    LessonResponse toDto(Lesson lesson);

}
