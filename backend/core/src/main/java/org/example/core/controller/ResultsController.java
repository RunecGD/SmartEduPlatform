package org.example.core.controller;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.*;
import org.example.core.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1")
public class ResultsController {
    private final ResultsService results;
    private final StudyGroupService groups;
    @GetMapping("/groups")
    public List<StudyGroupResponse> groups() { return groups.list(); }
    @GetMapping("/reports/courses/{courseId}")
    public List<StudentResultsResponse> course(@PathVariable Long courseId) { return results.course(courseId); }
    @GetMapping("/reports/groups/{groupId}")
    public List<StudentResultsResponse> group(@PathVariable Long groupId) { return results.group(groupId); }
}
