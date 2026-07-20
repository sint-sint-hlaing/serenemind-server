package com.mental.dto;

import lombok.Builder;

import java.time.LocalDateTime;
@Builder

public record AvatarResponse(
        Long id,
        String name,
        String imageUrl,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
