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

    // UI - Filter အလိုက် Notification List ဆွဲထုတ်ခြင်း
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UserPrincipal userPrincipal, String filter) {
        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Notification> notifications;

        // UI က Tab filter (All, Unread, Mentions, System) အပေါ်မူတည်ပြီး Query ခွဲထုတ်ခြင်း
        switch (filter.toLowerCase()) {
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

    // UI - Notification တစ်ခုကို Read ဖြစ်အောင် ပြောင်းလဲခြင်း
    @Transactional
    public void markAsRead(Long id, UserPrincipal userPrincipal) {
        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Notification notification = notificationRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // UI - Notification အားလုံးကို တစ်ခါတည်း Read ဖြစ်အောင် ပြောင်းလဲခြင်း
    @Transactional
    public void markAllAsRead(UserPrincipal userPrincipal) {
        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalse(currentUser);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    // Notification အသစ် ဆောက်ပေးမည့် Method
    @Transactional
    public void createNotification(User recipient, String title, String message, String type, Long targetId, String targetType) {
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

    // Noti ကို နှိပ်လိုက်လျှင် Read true လုပ်ပြီး Target အချက်အလက် ပြန်ပေးမည်
    @Transactional
    public NotificationResponse clickAndGetNotification(Long id, UserPrincipal userPrincipal) {
        User currentUser = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Notification notification = notificationRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // 👈 မဖတ်ရသေးရင် Read True ပြောင်းပေးရန်
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

        // 👈 Target Data များ ထည့်ပေးရန်
        response.setTargetId(notification.getTargetId());
        response.setTargetType(notification.getTargetType());

        return response;
    }

    public long getUnreadCount(Long id) {
        return notificationRepository.countByUserIdAndIsReadFalse(id);
    }

    public Page<NotificationResponse> getUnreadNotifications(UserPrincipal user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationPage= notificationRepository.findByUserIdAndIsReadFalse(user.getId(),pageable);
        return notificationPage.map(this::convertToNotificationResponse);
    }
}