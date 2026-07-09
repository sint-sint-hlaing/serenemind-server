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
    private String username;
    private String userProfilePicture;
    private int likeCount;
    private int commentCount;
    private boolean isLikedByMe;
    private LocalDateTime createdAt;
}
