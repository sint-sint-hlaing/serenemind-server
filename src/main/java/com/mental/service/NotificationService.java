package com.mental.service;

import com.mental.dto.Notification.NotificationResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.Notification;
import com.mental.model.entity.User;
import com.mental.repository.NotificationRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;


    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UserPrincipal userPrincipal, String filter) {
        if (userPrincipal == null) {
            throw new UsernameNotFoundException("Authenticated user not found");
        }

        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Notification> notifications;

        String selectedFilter = (filter != null) ? filter.toLowerCase() : "all";

        switch (selectedFilter) {
            case "unread":
                notifications = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(currentUser);
                break;
            case "mentions":
                notifications = notificationRepository.findByUserAndTypeOrderByCreatedAtDesc(currentUser, "MENTION");
                break;
            case "system":
                notifications = notificationRepository.findByUserAndTypeOrderByCreatedAtDesc(currentUser, "SYSTEM");
                break;
            case "all":
            default:
                notifications = notificationRepository.findByUserOrderByCreatedAtDesc(currentUser);
                break;
        }

        return notifications.stream()
                .map(this::convertToNotificationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long id, UserPrincipal userPrincipal) {
        if (id == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }
        if (userPrincipal == null) {
            throw new UsernameNotFoundException("Authenticated user not found");
        }

        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Notification notification = notificationRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        notification.setRead(true);
        notificationRepository.save(notification);
    }


    @Transactional
    public void markAllAsRead(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new UsernameNotFoundException("Authenticated user not found");
        }

        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalse(currentUser);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    @Transactional
    public void createNotification(User recipient, String title, String message, String type, Long targetId, String targetType) {
        if (recipient == null) {
            throw new IllegalArgumentException("Notification recipient user cannot be null");
        }

        Notification noti = new Notification();
        noti.setUser(recipient);
        noti.setTitle(title);
        noti.setMessage(message);
        noti.setType(type);
        noti.setRead(false);
        noti.setTargetId(targetId);
        noti.setTargetType(targetType);

        notificationRepository.save(noti);

        fcmPushService.sendPushNotificationToUser(recipient, title, message, targetId, targetType);
    }

    @Transactional
    public NotificationResponse clickAndGetNotification(Long id, UserPrincipal userPrincipal) {
        if (id == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }
        if (userPrincipal == null) {
            throw new UsernameNotFoundException("Authenticated user not found");
        }

        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Notification notification = notificationRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));


        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }

        return convertToNotificationResponse(notification);
    }

    private NotificationResponse convertToNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setRead(notification.isRead());
        if (notification.getCreatedAt() != null) {
            response.setCreatedAt(notification.getCreatedAt());
        }

        response.setTargetId(notification.getTargetId());
        response.setTargetType(notification.getTargetType());

        return response;
    }

    public long getUnreadCount(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return notificationRepository.countByUserIdAndIsReadFalse(id);
    }

    public Page<NotificationResponse> getUnreadNotifications(UserPrincipal user, int page, int size) {
        if (user == null) {
            throw new UsernameNotFoundException("Authenticated user not found");
        }
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page index must not be less than zero and size must be greater than zero");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndIsReadFalse(user.getId(), pageable);
        return notificationPage.map(this::convertToNotificationResponse);
    }
}