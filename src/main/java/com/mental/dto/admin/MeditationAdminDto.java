package com.mental.dto.admin;

import com.mental.model.entity.enums.MeditationTime;
import lombok.Builder;
import java.time.Instant;

@Builder
public record MeditationAdminDto(
        Long id,
        String title,
        String description,
        String category,
        Integer duration,
        String audioUrl,
        String imageUrl,
        boolean active,
        Long totalSessions,
        Instant createdAt,
        String timeOfDay,
        String status,
        Boolean featured,
        Boolean premium
) {
}