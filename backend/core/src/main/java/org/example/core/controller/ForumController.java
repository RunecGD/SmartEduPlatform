package org.example.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.ForumPostRequest;
import org.example.core.dto.request.ForumTopicRequest;
import org.example.core.dto.request.PostReactionRequest;
import org.example.core.dto.response.ForumPostResponse;
import org.example.core.dto.response.ForumTopicResponse;
import org.example.core.service.ForumService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @PostMapping("/courses/{courseId}/topics")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ForumTopicResponse createTopic(
            @PathVariable Long courseId,
            @Valid @RequestBody ForumTopicRequest request
    ) {
        return forumService.createTopic(
                courseId,
                request
        );
    }

    @GetMapping("/courses/{courseId}/topics")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public List<ForumTopicResponse> getTopics(
            @PathVariable Long courseId
    ) {
        return forumService.getTopics(courseId);
    }

    @GetMapping("/topics/{topicId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ForumTopicResponse getTopic(
            @PathVariable Long topicId
    ) {
        return forumService.getTopic(topicId);
    }

    @PostMapping("/topics/{topicId}/posts")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ForumPostResponse createPost(
            @PathVariable Long topicId,
            @Valid @RequestBody ForumPostRequest request
    ) {
        return forumService.createPost(
                topicId,
                request
        );
    }

    @PostMapping("/posts/{postId}/reactions")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addReaction(
            @PathVariable Long postId,
            @Valid @RequestBody PostReactionRequest request
    ) {
        forumService.addReaction(
                postId,
                request
        );
    }

    @DeleteMapping("/posts/{postId}/reactions")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReaction(
            @PathVariable Long postId
    ) {
        forumService.removeReaction(postId);
    }
}
