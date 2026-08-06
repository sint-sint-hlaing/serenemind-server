package com.mental.service;

import com.mental.dto.Comment.CommentRequest;
import com.mental.dto.Comment.CommentResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.CommentMapper;
import com.mental.model.entity.Comment;
import com.mental.model.entity.Post;
import com.mental.model.entity.User;
import com.mental.repository.CommentRepository;
import com.mental.repository.PostRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final CommentNotificationHandler notificationHandler;
    private final StreakService streakService;

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(Long postId, UserPrincipal userPrincipal) {
        log.debug("Fetching comments for post: {}", postId);

        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }

        User currentUser = getUserOrThrow(userPrincipal.getEmail());
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);

        Map<Long, Integer> anonymousUserMap = buildAnonymousUserMap(comments);

        return comments.stream()
                .map(comment -> commentMapper.toResponse(comment, anonymousUserMap, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse createComment(Long postId, UserPrincipal userPrincipal, CommentRequest request) {
        log.info("Creating comment for post: {} by user: {}", postId, userPrincipal.getEmail());

        Post post = getPostOrThrow(postId);
        User user = getUserOrThrow(userPrincipal.getEmail());

        Comment comment = commentMapper.toEntity(request, post, user);
        Comment savedComment = commentRepository.save(comment);

        incrementPostCommentCount(post);
        notificationHandler.notifyPostOwner(post, savedComment, user);
        streakService.updateStreak(userPrincipal.getEmail());

        return commentMapper.toResponse(savedComment, new HashMap<>(), user);
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    private void incrementPostCommentCount(Post post) {
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
    }

    private Map<Long, Integer> buildAnonymousUserMap(List<Comment> comments) {
        Map<Long, Integer> anonymousUserMap = new HashMap<>();
        int anonymousCounter = 1;

        for (Comment comment : comments) {
            if (comment.isAnonymous()) {
                Long userId = comment.getUser().getId();
                anonymousUserMap.putIfAbsent(userId, anonymousCounter++);
            }
        }

        return anonymousUserMap;
    }
}