package org.example.core.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.core.config.AiServiceClient;
import org.example.core.dto.response.MaterialResponse;
import org.example.core.model.Course;
import org.example.core.model.Lesson;
import org.example.core.model.Material;
import org.example.core.model.User;
import org.example.core.repository.LessonRepository;
import org.example.core.repository.MaterialRepository;
import org.example.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MinioClient minioClient;
    private final MaterialRepository materialRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiServiceClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Transactional
    public MaterialResponse upload(Long lessonId, MultipartFile file) throws Exception {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Урок не найден"));
        checkOwner(lesson.getModule().getCourse());

        // убеждаемся, что бакет есть (первый запуск)
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        String objectKey = "materials/%d/%s".formatted(lessonId, UUID.randomUUID()
                + "_" + StringUtils.cleanPath(file.getOriginalFilename()));

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        Material material = Material.builder()
                .lesson(lesson)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .objectKey(objectKey)
                .sizeBytes(file.getSize())
                .build();

        Material saved = materialRepository.save(material);
        try {
            aiServiceClient.index(objectKey);
        } catch (Exception e) {
            // лог: материал сохранён, но не проиндексирован — повторим позже
        }        return new MaterialResponse(saved.getId(), lessonId, saved.getFileName(),
                saved.getContentType(), saved.getSizeBytes(), saved.getUploadedAt());
    }

    public String downloadUrl(Long materialId) throws Exception {
        Material m = materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("Материал не найден"));
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(m.getObjectKey())
                .expiry(10, TimeUnit.MINUTES)
                .build());
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    }

    private void checkOwner(Course course) {
        if (!course.getTeacher().getId().equals(currentUser().getId())
                && !isAdmin()) {
            throw new AccessDeniedException("Недостаточно прав");
        }
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }}
