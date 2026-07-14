package com.mental.dto.Comment;


import lombok.*;
import java.time.LocalDateTime;


@Getter @Setter @Builder
@NoArgsConstructor       // Default Constructor အတွက်
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private boolean isAnonymous;
    private String username;
    private String userProfilePicture;
    private LocalDateTime createdAt;
}
