package org.example.core.controller;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.repository.LessonRepository;
import org.example.core.service.AccessGuard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class AiLessonController {
    private final LessonRepository lessons;
    private final AccessGuard guard;
    private final JdbcTemplate jdbc;
    public record AiLessonStatus(int materialCount, int readyCount, int pendingCount, int retryingCount) {}
    @GetMapping("/api/v1/lessons/{lessonId}/ai-status") @Transactional(readOnly=true)
    public AiLessonStatus status(@PathVariable Long lessonId) {
        var lesson = lessons.findById(lessonId).orElseThrow(() -> new EntityNotFoundException("Урок не найден"));
        guard.courseAccess(lesson.getModule().getCourse());
        return jdbc.queryForObject("""
            SELECT count(*) AS total,
                count(*) FILTER (WHERE EXISTS (SELECT 1 FROM material_chunks mc WHERE mc.material_id=m.id)) AS ready,
                count(*) FILTER (WHERE EXISTS (SELECT 1 FROM smartedu_jobs j WHERE j.kind='INDEX' AND j.payload=m.object_key AND j.attempts=0)) AS pending,
                count(*) FILTER (WHERE EXISTS (SELECT 1 FROM smartedu_jobs j WHERE j.kind='INDEX' AND j.payload=m.object_key AND j.attempts>0)) AS retrying
            FROM materials m WHERE m.lesson_id=?
            """, (rs,n) -> new AiLessonStatus(rs.getInt("total"), rs.getInt("ready"),
                    rs.getInt("pending"), rs.getInt("retrying")), lessonId);
    }
}
