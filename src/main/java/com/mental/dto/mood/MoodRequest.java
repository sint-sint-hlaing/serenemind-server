package com.mental.dto.mood;

import com.mental.model.entity.enums.MoodType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MoodRequest(
        @NotNull(message = "Mood is required")
        MoodType mood,

        @NotNull(message = "Intensity is required")
        @Min(value = 1, message = "Intensity must be between 1 and 10")
        @Max(value = 10, message = "Intensity must be between 1 and 10")
        int intensity,

        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note

) {
}
