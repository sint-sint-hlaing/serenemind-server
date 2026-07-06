package com.mental.dto.mood;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        String username,
        String action,
        Long targetId,
        String details,
        LocalDateTime createdAt
) {
}