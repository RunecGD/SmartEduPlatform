package org.example.core.dto.response;
import java.util.List;
public record StudyGroupResponse(Long id, String name, Long curatorId, String curatorName,
                                 List<UserResponse> students) {}
