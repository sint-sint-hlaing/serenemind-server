package com.mental.controller;

import com.mental.dto.Notification.NotificationResponse;
import com.mental.dto.NotificationDto;
import com.mental.model.entity.Notification;
import com.mental.security.UserPrincipal;
import com.mental.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // UI - Notifications Screen (All, Unread, Mentions, System filter များအတွက်)
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "all") String filter) {

        List<NotificationResponse> notifications = notificationService.getNotifications(userPrincipal, filter);
        return ResponseEntity.ok(notifications);
    }

    // UI - Notification တစ်ခုချင်းစီကို ဖတ်ပြီးကြောင်း Mark လုပ်ရန် (အစက်အပြာလေး ပျောက်သွားစေရန်)
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        notificationService.markAsRead(id, userPrincipal);
        return ResponseEntity.ok().build();
    }

    // UI - Notification အားလုံးကို တစ်ခါတည်း အဖတ်မှတ်ရန် (Clear or Mark All as Read)
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.markAllAsRead(userPrincipal);
        return ResponseEntity.ok().build();
    }

//    // UI/Testing - Notification အသစ် ဆောက်ရန် (Create Notification)
//    @PostMapping
//    public ResponseEntity<NotificationResponse> createNotification(
//            @AuthenticationPrincipal UserPrincipal userPrincipal,
//            @jakarta.validation.Valid @RequestBody com.mental.dto.Notification.NotificationRequest request) {
//
//        NotificationResponse response = notificationService.createNotification(userPrincipal, request);
//        return  ResponseEntity.ok(response);
//    }

    @GetMapping("/{id}/click")
    public ResponseEntity<NotificationResponse> clickNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        NotificationResponse response = notificationService.clickAndGetNotification(id, userPrincipal);
        return ResponseEntity.ok(response);
    }
}
