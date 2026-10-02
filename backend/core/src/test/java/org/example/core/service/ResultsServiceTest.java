package org.example.core.service;
import org.example.core.dto.enums.Role;
import org.example.core.model.*;
import org.example.core.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResultsServiceTest {
    UserRepository users=mock(UserRepository.class);
    EnrollmentRepository enrollments=mock(EnrollmentRepository.class);
    StudyGroupRepository groups=mock(StudyGroupRepository.class);
    CourseRepository courses=mock(CourseRepository.class);
    ExamAttemptRepository attempts=mock(ExamAttemptRepository.class);
    LessonRepository lessons=mock(LessonRepository.class);
    LessonProgressRepository lessonProgress=mock(LessonProgressRepository.class);
    AccessGuard guard=new AccessGuard(users,enrollments,groups);
    ProgressService progress=new ProgressService(guard,enrollments,lessons,lessonProgress,attempts);
    ResultsService service=new ResultsService(guard,courses,groups,enrollments,attempts,progress);
    User teacher=User.builder().id(1L).email("teacher@test.local").role(Role.TEACHER).build();
    User student=User.builder().id(2L).fullName("Student").email("student@test.local").role(Role.STUDENT).build();
    Course course=Course.builder().id(10L).title("Own course").teacher(teacher).build();
    @BeforeEach void login(){
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(teacher.getEmail(),null,List.of()));
        when(users.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(courses.findById(10L)).thenReturn(Optional.of(course));
    }
    @AfterEach void logout(){SecurityContextHolder.clearContext();}
    @Test void teacherReportNeverLoadsStudentsOtherCourses(){
        var enrollment=Enrollment.builder().id(100L).course(course).user(student).build();
        when(enrollments.findByCourseId(10L)).thenReturn(List.of(enrollment));
        var result=service.course(10L);
        assertEquals(1,result.size()); assertEquals(2L,result.getFirst().userId());
        assertEquals(1,result.getFirst().courses().size());
        assertEquals(10L,result.getFirst().courses().getFirst().progress().courseId());
        verify(attempts).findByUser_IdAndExam_Lesson_Module_Course_IdOrderByStartedAtDesc(2L,10L);
        verify(enrollments,never()).findByUserId(anyLong());
        verify(attempts,never()).findByUser_Id(anyLong());
        verify(attempts,never()).findAll();
    }
    @Test void foreignCourseDeniedBeforeReadingStudents(){
        course.setTeacher(User.builder().id(3L).build());
        assertThrows(AccessDeniedException.class,()->service.course(10L));
        verifyNoInteractions(enrollments,attempts);
    }
    @Test void curatorCannotReadAnotherGroupEvenWithKnownId(){
        teacher.setRole(Role.METHODIST);
        var group=new StudyGroup();group.setId(7L);group.setCurator(User.builder().id(99L).build());
        group.getStudents().add(student);when(groups.findById(7L)).thenReturn(Optional.of(group));
        assertThrows(AccessDeniedException.class,()->service.group(7L));
        verifyNoInteractions(enrollments,attempts);
    }
    @Test void curatorSeesUnenrolledMemberWithoutInventingProgress(){
        teacher.setRole(Role.METHODIST);
        var group=new StudyGroup();group.setId(7L);group.setCurator(teacher);group.getStudents().add(student);
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        var result=service.group(7L);
        assertEquals(2L,result.getFirst().userId()); assertTrue(result.getFirst().courses().isEmpty());
        verify(enrollments).findByUserId(2L);verifyNoInteractions(attempts);
    }
}
