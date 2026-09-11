package org.example.core.service;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.enums.Role;
import org.example.core.dto.request.UserRequest;
import org.example.core.dto.response.UserResponse;
import org.example.core.mapper.UserMapper;
import org.example.core.model.User;
import org.example.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse create(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.email())) {
            throw new IllegalArgumentException("Email уже зарегистрирован");
        }

        User user = User.builder()
                .fullName(userRequest.fullName())
                .email(userRequest.email())
                .passwordHash(passwordEncoder.encode(userRequest.password()))
                .role(Role.STUDENT)
                .isEnabled(true)
                .build();

        return userMapper.toDto(userRepository.save(user));
    }
}