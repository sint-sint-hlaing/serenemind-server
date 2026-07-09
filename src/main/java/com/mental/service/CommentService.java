package com.mental.service;

import com.mental.dto.Comment.CommentRequest;
import com.mental.dto.Comment.CommentResponse;
import com.mental.model.entity.Comment;
import com.mental.model.entity.Post;
import com.mental.model.entity.User;
import com.mental.repository.CommentRepository;
import com.mental.repository.PostRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.mental.exception.ResourceNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // UI - သက်ဆိုင်ရာ Post ID အလိုက် ကွန်မန့်များအားလုံး ဆွဲထုတ်ခြင်း
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(Long postId, UserPrincipal userPrincipal) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }

        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
    }

    // UI - ကွန်မန့်အသစ် ရေးသားခြင်း
    @Transactional
    public CommentResponse createComment(Long postId, UserPrincipal userPrincipal, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        // ပို့စ်၏ Comment ရေတွက်မှုကို တစ်ခါတည်း တိုးပေးခြင်း
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        return convertToCommentResponse(savedComment);
    }

    // Helper Method: Comment Entity မှ CommentResponse DTO သို့ ပြောင်းလဲခြင်း
    private CommentResponse convertToCommentResponse(Comment comment) {
        String profilePic = (comment.getUser().getUserProfile() != null)
                ? comment.getUser().getUserProfile().getAvatar()
                : null;

        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setUsername(comment.getUser().getUsername());
        response.setUserProfilePicture(profilePic);
        response.setCreatedAt(comment.getCreatedAt());

        return response;
    }
}