package com.mental.dto.Post;


import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
public class PostRequest {
    @NotBlank(message = "Content cannot be empty")
    private String content;
    private String imageUrl;
    private boolean isAnonymous;
}
