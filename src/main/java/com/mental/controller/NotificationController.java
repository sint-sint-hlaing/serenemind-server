package com.mental.controller;

import com.mental.dto.NotificationDto;
import com.mental.model.entity.Notification;
import com.mental.security.UserPrincipal;
import com.mental.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody NotificationDto request, UserPrincipal userPrinciple) {
        return ResponseEntity.ok(notificationService.createNotification(userPrinciple.getUsername(),request));
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getAllNotifications(UserPrincipal userPrinciple) {
        return ResponseEntity.ok(notificationService.getUserNotification(userPrinciple.getUsername()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }}

