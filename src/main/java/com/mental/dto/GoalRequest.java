package com.mental.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GoalRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @Min(value = 1, message = "Target days must be at least 1")
        int targetDays
) {}