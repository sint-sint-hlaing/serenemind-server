package com.mental.mapper;

import com.mental.dto.Comment.CommentRequest;
import com.mental.dto.Comment.CommentResponse;
import com.mental.model.entity.Comment;
import com.mental.model.entity.Post;
import com.mental.model.entity.User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommentMapper {

    public Comment toEntity(CommentRequest request, Post post, User user) {
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(user);
        comment.setAnonymous(request.isAnonymous());
        return comment;
    }

    public CommentResponse toResponse(Comment comment, Map<Long, Integer> anonymousUserMap, User currentUser) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        response.setAnonymous(comment.isAnonymous());

        if (comment.isAnonymous()) {
            Integer anonymousNumber = anonymousUserMap.getOrDefault(comment.getUser().getId(), 1);
            String displayName = "Anonymous " + anonymousNumber;

            // Check if commenter is post author
            if (comment.getUser().getId().equals(comment.getPost().getUser().getId())) {
                displayName += " (Author)";
            }

            // Check if commenter is current user
            if (comment.getUser().getId().equals(currentUser.getId())) {
                displayName += " (You)";
            }

            response.setUsername(displayName);
            response.setUserProfilePicture(null);
        } else {
            response.setUsername(comment.getUser().getUsername());
            response.setUserProfilePicture(
                    comment.getUser().getUserProfile() != null
                            ? comment.getUser().getUserProfile().getProfileImageUrl()
                            : null
            );
        }

        return response;
    }
}