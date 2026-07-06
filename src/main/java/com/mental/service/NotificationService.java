package com.mental.service;

import com.mental.dto.NotificationDto;
import com.mental.model.entity.Notification;

import java.util.List;

public interface NotificationService {
    Notification createNotification(String username, NotificationDto request);

    List<NotificationDto> getUserNotification(String username);

    void markAsRead(Long id);
}
