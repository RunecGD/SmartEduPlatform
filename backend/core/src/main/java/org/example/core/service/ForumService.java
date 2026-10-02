package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.config.EventPublisher;
import org.example.core.dto.request.ForumPostRequest;
import org.example.core.dto.request.ForumReplyEvent;
import org.example.core.dto.request.ForumTopicRequest;
import org.example.core.dto.request.PostReactionRequest;
import org.example.core.dto.response.ForumPostResponse;
import org.example.core.dto.response.ForumTopicResponse;
import org.example.core.model.Course;
import org.example.core.model.ForumPost;
import org.example.core.model.ForumTopic;
import org.example.core.model.PostReaction;
import org.example.core.model.User;
import org.example.core.repository.CourseRepository;
import org.example.core.repository.ForumPostRepository;
import org.example.core.repository.ForumTopicRepository;
import org.example.core.repository.PostReactionRepository;
import org.example.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final jakarta.persistence.EntityManager entityManager;
    private final AccessGuard accessGuard;
    private final ForumTopicRepository topicRepository;
    private final ForumPostRepository postRepository;
    private final PostReactionRepository reactionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ForumTopicResponse createTopic(
            Long courseId,
            ForumTopicRequest request
    ) {

        User user = currentUser();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Курс не найден"
                        )
                );

        checkCourseAccess(course, user);

        ForumTopic topic = ForumTopic.builder()
                .course(course)
                .author(user)
                .title(request.title())
                .build();

        return toTopicResponse(
                topicRepository.save(topic)
        );
    }

    @Transactional(readOnly = true)
    public List<ForumTopicResponse> getTopics(
            Long courseId
    ) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Курс не найден"
                        )
                );

        accessGuard.courseAccess(course);

        return topicRepository
                .findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toTopicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ForumTopicResponse getTopic(
            Long topicId
    ) {

        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Тема не найдена"
                        )
                );

        accessGuard.courseAccess(topic.getCourse());
        return toTopicResponse(topic);
    }

    @Transactional
    public ForumPostResponse createPost(
            Long topicId,
            ForumPostRequest request
    ) {
        User user = currentUser();

        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Тема не найдена"
                        )
                );

        checkCourseAccess(
                topic.getCourse(),
                user
        );

        ForumPost post = ForumPost.builder()
                .topic(topic)
                .author(user)
                .content(request.content())
                .build();

        ForumPost saved = postRepository.save(post);

        if (!topic.getAuthor().getId().equals(user.getId())) {

            eventPublisher.publishForumReply(
                    new ForumReplyEvent(
                            topic.getAuthor().getId(),
                            topic.getId(),
                            saved.getId()
                    )
            );
        }

        return toPostResponse(saved);
    }

    @Transactional
    public void addReaction(
            Long postId,
            PostReactionRequest request
    ) {

        User user = currentUser();

        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Пост не найден"
                        )
                );

        checkCourseAccess(
                post.getTopic().getCourse(),
                user
        );

        entityManager.lock(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        PostReaction reaction =
                reactionRepository
                        .findByPostIdAndUserId(
                                postId,
                                user.getId()
                        )
                        .orElse(
                                PostReaction.builder()
                                        .post(post)
                                        .user(user)
                                        .build()
                        );

        reaction.setReaction(request.reaction());

        reactionRepository.save(reaction);
    }

    @Transactional
    public void removeReaction(Long postId) {

        User user = currentUser();

        entityManager.lock(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        PostReaction reaction =
                reactionRepository
                        .findByPostIdAndUserId(
                                postId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Реакция не найдена"
                                )
                        );

        reactionRepository.delete(reaction);
    }

    private ForumTopicResponse toTopicResponse(
            ForumTopic topic
    ) {

        List<ForumPostResponse> posts =
                (topic.getPosts() == null ? java.util.stream.Stream.<ForumPost>empty() : topic.getPosts().stream())
                        .map(this::toPostResponse)
                        .toList();

        return new ForumTopicResponse(
                topic.getId(),
                topic.getCourse().getId(),
                topic.getAuthor().getId(),
                topic.getAuthor().getFullName(),
                topic.getTitle(),
                topic.getCreatedAt(),
                topic.getUpdatedAt(),
                posts
        );
    }

    private ForumPostResponse toPostResponse(
            ForumPost post
    ) {

        return new ForumPostResponse(
                post.getId(),
                post.getTopic().getId(),
                post.getAuthor().getId(),
                post.getAuthor().getFullName(),
                post.getContent(),
                post.getCreatedAt()
        );
    }

    private void checkCourseAccess(
            Course course,
            User user
    ) {

        accessGuard.courseAccess(course);
    }

    private User currentUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Пользователь не найден"
                        )
                );
    }
}
