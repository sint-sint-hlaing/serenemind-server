package com.mental.dto;

import lombok.Builder;

@Builder
public record NotificationDto(
        Long id,
        String title,
        String message,
        String type, // LOCAL or FCM
        boolean isRead
) {}
