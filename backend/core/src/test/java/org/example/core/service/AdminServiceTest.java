package org.example.core.service;
import org.example.core.dto.enums.*;
import org.example.core.dto.request.*;
import org.example.core.mapper.*;
import org.example.core.model.*;
import org.example.core.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AdminServiceTest {
    UserRepository users=mock(UserRepository.class);
    CourseRepository courses=mock(CourseRepository.class);
    StudyGroupRepository groups=mock(StudyGroupRepository.class);
    AccessGuard guard=mock(AccessGuard.class);
    AdminMutationLock lock=mock(AdminMutationLock.class);
    AdminService service=new AdminService(users,courses,groups,new UserMapper(),new CourseMapper(),guard,lock);
    @Test void adminCannotDisableOrDemoteSelf(){
        var admin=User.builder().id(1L).role(Role.ADMIN).build();
        when(guard.currentUser()).thenReturn(admin);when(users.findById(1L)).thenReturn(Optional.of(admin));
        assertThrows(IllegalStateException.class,()->service.updateUser(1L,new AdminUserRequest("Admin",Role.ADMIN,false)));
        assertThrows(IllegalStateException.class,()->service.updateUser(1L,new AdminUserRequest("Admin",Role.STUDENT,true)));
        verify(users,never()).saveAndFlush(any());
    }
    @Test void assignedCuratorCannotSilentlyLoseRole(){
        when(guard.currentUser()).thenReturn(User.builder().id(1L).role(Role.ADMIN).build());
        var curator=User.builder().id(2L).role(Role.METHODIST).build();
        when(users.findById(2L)).thenReturn(Optional.of(curator));when(groups.existsByCurator_Id(2L)).thenReturn(true);
        assertThrows(IllegalStateException.class,()->service.updateUser(2L,new AdminUserRequest("Curator",Role.STUDENT,true)));
        assertEquals(Role.METHODIST,curator.getRole());
    }
    @Test void courseCannotBeAssignedToStudent(){
        var teacher=User.builder().id(1L).role(Role.TEACHER).build();
        var course=Course.builder().id(10L).teacher(teacher).build();
        when(courses.findById(10L)).thenReturn(Optional.of(course));
        when(users.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).role(Role.STUDENT).build()));
        assertThrows(IllegalArgumentException.class,()->service.updateCourse(10L,new AdminCourseRequest(2L,CourseStatus.PUBLISHED)));
        assertEquals(teacher,course.getTeacher());
    }
}
