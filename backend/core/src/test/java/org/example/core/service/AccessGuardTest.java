package org.example.core.service;
import org.example.core.dto.enums.Role;
import org.example.core.model.*;
import org.example.core.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccessGuardTest {
 UserRepository users=mock(UserRepository.class);
 EnrollmentRepository enrollments=mock(EnrollmentRepository.class);
 StudyGroupRepository groups=mock(StudyGroupRepository.class);
 AccessGuard guard=new AccessGuard(users,enrollments,groups);
 User student=User.builder().id(7L).email("student@test.local").role(Role.STUDENT).build();
 Course course=Course.builder().id(10L).teacher(User.builder().id(8L).role(Role.TEACHER).build()).build();
 @BeforeEach void authenticate(){
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(student.getEmail(),null,List.of()));
  when(users.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
 }
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
 @Test void unrelatedStudentCannotReadCourse(){assertThrows(AccessDeniedException.class,()->guard.courseAccess(course));}
 @Test void enrolledStudentCanReadButCannotModify(){
  when(enrollments.existsByUserIdAndCourseId(7L,10L)).thenReturn(true);
  assertDoesNotThrow(()->guard.courseAccess(course));
  assertThrows(AccessDeniedException.class,()->guard.owner(course));
 }
 @Test void anotherTeachersCourseIsProtected(){student.setRole(Role.TEACHER);assertThrows(AccessDeniedException.class,()->guard.owner(course));}
 @Test void progressCannotBeQueriedWithForeignId(){assertThrows(AccessDeniedException.class,()->guard.ownProgress(8L));assertDoesNotThrow(()->guard.ownProgress(7L));}
 @Test void anonymousIsDenied(){SecurityContextHolder.clearContext();assertThrows(AccessDeniedException.class,guard::currentUser);}
 @Test void curatorOnlyReadsAssignedGroups(){
  student.setRole(Role.METHODIST);
  StudyGroup group=new StudyGroup(); group.setCurator(User.builder().id(8L).build());
  assertThrows(AccessDeniedException.class,()->guard.group(group));
  group.setCurator(student); assertDoesNotThrow(()->guard.group(group));
 }
 @Test void attemptResultsAreScopedByRoleAndAssignment(){
  var learner=User.builder().id(20L).role(Role.STUDENT).build();
  var lesson=Lesson.builder().module(CourseModule.builder().course(course).build()).build();
  var attempt=ExamAttempt.builder().user(learner).exam(Exam.builder().lesson(lesson).build()).build();
  assertThrows(AccessDeniedException.class,()->guard.attemptResult(attempt,student));
  assertDoesNotThrow(()->guard.attemptResult(attempt,learner));
  student.setRole(Role.TEACHER);
  assertThrows(AccessDeniedException.class,()->guard.attemptResult(attempt,student));
  assertDoesNotThrow(()->guard.attemptResult(attempt,course.getTeacher()));
  student.setRole(Role.METHODIST);
  assertThrows(AccessDeniedException.class,()->guard.attemptResult(attempt,student));
  when(groups.existsByCurator_IdAndStudents_Id(7L,20L)).thenReturn(true);
  assertDoesNotThrow(()->guard.attemptResult(attempt,student));
  when(groups.existsByCurator_IdAndStudents_Id(7L,20L)).thenReturn(false);
  assertThrows(AccessDeniedException.class,()->guard.attemptResult(attempt,student));
  student.setRole(Role.ADMIN); assertDoesNotThrow(()->guard.attemptResult(attempt,student));
  student.setIsEnabled(false); assertThrows(AccessDeniedException.class,()->guard.attemptResult(attempt,student));
 }
}
