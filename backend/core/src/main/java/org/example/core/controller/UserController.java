package org.example.core.controller;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.UserResponse;
import org.example.core.mapper.UserMapper;
import org.example.core.service.AccessGuard;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final AccessGuard accessGuard;
    private final UserMapper userMapper;
    @GetMapping("/me")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserResponse me() { return userMapper.toDto(accessGuard.currentUser()); }
}
