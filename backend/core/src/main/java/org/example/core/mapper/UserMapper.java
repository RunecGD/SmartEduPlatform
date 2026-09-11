package org.example.core.mapper;

import org.example.core.dto.response.UserResponse;
import org.example.core.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toDto(User user);
}