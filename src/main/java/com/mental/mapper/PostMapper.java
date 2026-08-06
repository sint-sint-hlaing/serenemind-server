package com.mental.mapper;

import com.mental.dto.Post.PostResponse;
import com.mental.model.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toPostResponse(Post post) {
        if (post == null) {
            return null;
        }

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .isAnonymous(post.isAnonymous())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .username(post.isAnonymous() ? "Anonymous" : post.getUser().getUsername())
                .userProfilePicture(post.getImageUrl())
                .isLikedByMe(false)
                .build();
    }
}