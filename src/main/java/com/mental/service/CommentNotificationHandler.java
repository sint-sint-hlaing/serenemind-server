package com.mental.service;

import com.mental.model.entity.Comment;
import com.mental.model.entity.Post;
import com.mental.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentNotificationHandler {

    private final NotificationService notificationService;

    public void notifyPostOwner(Post post, Comment comment, User commenter) {
        // Don't notify if commenter is the post owner
        if (post.getUser().getId().equals(commenter.getId())) {
            return;
        }

        String commenterName = comment.isAnonymous() ? "Anonymous" : commenter.getUsername();

        notificationService.createNotification(
                post.getUser(),
                "New comment on your post",
                commenterName + " commented: \"" + truncateContent(comment.getContent()) + "\"",
                "COMMENT",
                post.getId(),
                "COMMENT"
        );
    }

    private String truncateContent(String content) {
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }
}