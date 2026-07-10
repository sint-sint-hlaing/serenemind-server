package com.mental.dto.Breathing;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BreathingSessionRequest {
    @NotBlank(message = "Exercise type is required")
    private String exerciseType; // e.g., "BOX_BREATHING", "FOUR_SEVEN_EIGHT", "CALM"

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes; // e.g., 1, 3, 5, 10
}