package org.example.core.mapper;
import org.example.core.model.User;
import org.example.core.dto.response.UserResponse;
import org.springframework.stereotype.Component;
@Component
public class UserMapper {
 public UserResponse toDto(User entity) { return new UserResponse(entity.getId(), entity.getEmail(), entity.getFullName(), entity.getRole(), entity.getIsEnabled(), entity.getCreatedAt(), entity.getUpdatedAt()); }
}
