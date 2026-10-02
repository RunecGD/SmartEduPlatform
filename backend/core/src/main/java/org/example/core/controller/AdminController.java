package org.example.core.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.*;
import org.example.core.dto.response.*;
import org.example.core.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService admin;
    private final StudyGroupService groups;
    @GetMapping("/users") public List<UserResponse> users() { return admin.users(); }
    @PatchMapping("/users/{id}") public UserResponse user(@PathVariable Long id, @Valid @RequestBody AdminUserRequest request) { return admin.updateUser(id, request); }
    @GetMapping("/courses") public List<CourseResponse> courses() { return admin.courses(); }
    @PatchMapping("/courses/{id}") public CourseResponse course(@PathVariable Long id, @Valid @RequestBody AdminCourseRequest request) { return admin.updateCourse(id, request); }
    @PostMapping("/groups") @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse createGroup(@Valid @RequestBody StudyGroupRequest request) { return groups.save(null, request); }
    @PutMapping("/groups/{id}") public StudyGroupResponse group(@PathVariable Long id, @Valid @RequestBody StudyGroupRequest request) { return groups.save(id, request); }
}
