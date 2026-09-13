package org.example.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = Instant.now();
    }
}