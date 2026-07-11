package com.mental.dto.Reminder;

import lombok.Builder;

@Builder
public record MeditationDto(
        Long id,
        String title,
        String duration,
        String category,
        String audioUrl, // Added this field
        String imageUrl
) {
}
