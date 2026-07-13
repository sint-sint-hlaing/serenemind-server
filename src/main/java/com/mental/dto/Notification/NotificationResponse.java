package com.mental.dto.Notification;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private Long targetId;
    private String targetType;
    private LocalDateTime createdAt;
}