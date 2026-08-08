package com.mental.controller;

import com.mental.dto.Notification.NotificationResponse;
import com.mental.model.entity.User;
import com.mental.security.UserPrincipal;
import com.mental.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "all") String filter) {

        List<NotificationResponse> notifications = notificationService.getNotifications(userPrincipal, filter);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        notificationService.markAsRead(id, userPrincipal);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.markAllAsRead(userPrincipal);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserPrincipal user){
        long userId=user.getId();
        long count= notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(count);
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Page<NotificationResponse> notifications= notificationService.getUnreadNotifications(user, page, size);
        return ResponseEntity.ok(notifications);
    }


    @GetMapping("/{id}/click")
    public ResponseEntity<NotificationResponse> clickNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        NotificationResponse response = notificationService.clickAndGetNotification(id, userPrincipal);
        return ResponseEntity.ok(response);
    }

}
