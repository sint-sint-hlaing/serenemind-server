package com.mental.dto.meditation;

import lombok.Builder;

@Builder
public record MeditationResponse(
        Long id,
        String title,
        String description,
        String category,
        String duration,
        String audioUrl,
        String imageUrl
) {
}
