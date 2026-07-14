package com.mental.dto.Comment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
public class CommentRequest {
    @NotBlank(message = "Comment cannot be empty")
    private String content;
    private boolean isAnonymous;
}
