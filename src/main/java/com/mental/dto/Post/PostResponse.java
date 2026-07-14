package com.mental.dto.Post;


import lombok.*;
import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor       // Default Constructor အတွက်
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String content;
    private String imageUrl;
    private boolean isAnonymous;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private String username;
    private String userProfilePicture;
    private boolean isLikedByMe;
}
