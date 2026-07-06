package com.mental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JournalRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;
}
