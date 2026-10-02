package org.example.core.controller;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.*;
import org.example.core.mapper.*;
import org.example.core.repository.*;
import org.example.core.service.AccessGuard;
import org.example.core.dto.enums.AttemptStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import jakarta.persistence.EntityNotFoundException;
@RestController @RequiredArgsConstructor @Transactional(readOnly=true)
public class LearningReadController {
 private final AccessGuard guard;
 private final CourseRepository courses; private final CourseModuleRepository modules;
 private final LessonRepository lessons; private final MaterialRepository materials;
 private final ExamRepository exams; private final ExamQuestionRepository questions; private final ExamAttemptRepository attempts;
 private final CourseMapper courseMapper; private final CourseModuleMapper moduleMapper; private final LessonMapper lessonMapper;
 @GetMapping("/api/v1/courses/mine")
 @org.springframework.security.access.prepost.PreAuthorize("hasRole('TEACHER')")
 public List<CourseResponse> mine() { return courses.findByTeacherId(guard.currentUser().getId()).stream().map(courseMapper::toDto).toList(); }
 @GetMapping("/api/v1/course_modules/{courseId}")
 public List<CourseModuleResponse> modules(@PathVariable Long courseId) {
  var course=courses.findById(courseId).orElseThrow(()->new EntityNotFoundException("Курс не найден"));guard.courseAccess(course);
  return modules.findByCourseId(courseId).stream().sorted(java.util.Comparator.comparing(org.example.core.model.CourseModule::getOrderIndex)).map(moduleMapper::toDto).toList();
 }
 @GetMapping("/api/v1/lessons/modules/{moduleId}")
 public List<LessonResponse> lessons(@PathVariable Long moduleId) {
  var module=modules.findById(moduleId).orElseThrow(()->new EntityNotFoundException("Модуль не найден"));guard.courseAccess(module.getCourse());
  return module.getLessons().stream().map(lessonMapper::toDto).toList();
 }
 @GetMapping("/api/v1/lessons/{lessonId}")
 public LessonResponse lesson(@PathVariable Long lessonId) { var lesson=lessons.findById(lessonId).orElseThrow(()->new EntityNotFoundException("Урок не найден"));guard.courseAccess(lesson.getModule().getCourse());return lessonMapper.toDto(lesson); }
 @GetMapping("/api/v1/material/{lessonId}")
 public List<MaterialResponse> materials(@PathVariable Long lessonId) {
  var lesson=lessons.findById(lessonId).orElseThrow(()->new EntityNotFoundException("Урок не найден"));guard.courseAccess(lesson.getModule().getCourse());
  return materials.findByLesson_Id(lessonId).stream().map(m->new MaterialResponse(m.getId(),lessonId,m.getFileName(),m.getContentType(),m.getSizeBytes(),m.getUploadedAt())).toList();
 }
 @GetMapping("/api/v1/exems/lessons/{lessonId}")
 public List<ExamResponse> exams(@PathVariable Long lessonId) {
  var lesson=lessons.findById(lessonId).orElseThrow(()->new EntityNotFoundException("Урок не найден"));guard.courseAccess(lesson.getModule().getCourse());
  return exams.findByLesson_Id(lessonId).stream().map(e->new ExamResponse(e.getId(),lessonId,e.getTitle(),e.getTimeLimitMinutes())).toList();
 }
 @GetMapping("/api/v1/exems/{examId}")
 public ExamResponse exam(@PathVariable Long examId) { var exam=exams.findById(examId).orElseThrow(()->new EntityNotFoundException("Экзамен не найден"));guard.courseAccess(exam.getLesson().getModule().getCourse());return new ExamResponse(exam.getId(),exam.getLesson().getId(),exam.getTitle(),exam.getTimeLimitMinutes()); }
 @GetMapping("/api/v1/exems/{examId}/questions")
 public List<ExamQuestionResponse> questions(@PathVariable Long examId) {
  var exam=exams.findById(examId).orElseThrow(()->new EntityNotFoundException("Экзамен не найден"));var course=exam.getLesson().getModule().getCourse();guard.courseAccess(course);var user=guard.currentUser();
  if (user.getRole()!=org.example.core.dto.enums.Role.ADMIN && !course.getTeacher().getId().equals(user.getId()) && !attempts.existsByExamIdAndUserIdAndStatus(examId,user.getId(),AttemptStatus.IN_PROGRESS)) throw new org.springframework.security.access.AccessDeniedException("Сначала начните попытку");
  return questions.findByExamIdOrderByOrderIndexAsc(examId).stream().map(q->new ExamQuestionResponse(q.getId(),q.getQuestionText(),q.getMaxScore(),q.getOrderIndex())).toList();
 }
}
