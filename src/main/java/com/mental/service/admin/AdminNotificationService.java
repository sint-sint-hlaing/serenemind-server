package com.mental.service.admin;

import com.mental.dto.Notification.NotificationRequest;
import com.mental.dto.admin.NotificationDto;

public interface AdminNotificationService {
    NotificationDto createNotification(NotificationRequest request);
    // Notification Management
    

}
