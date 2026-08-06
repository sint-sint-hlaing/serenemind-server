package com.mental.dto.admin;


import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;


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

        Instant createdAt

) {

}