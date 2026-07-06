package com.mental.service.impl;

import com.mental.dto.NotificationDto;
import com.mental.model.entity.Notification;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.NotificationType;
import com.mental.repository.NotificationRepository;
import com.mental.repository.UserRepository;
import com.mental.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Notification createNotification(String username, NotificationDto request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        // Enum ကို String မှ Enum သို့ ပြောင်းလဲခြင်း
        notification.setType(NotificationType.valueOf(request.type()));
        notification.setRead(false); // အသစ်ဖန်တီးလျှင် default အနေဖြင့် မဖတ်ရသေးဟု သတ်မှတ်သည်

        return repository.save(notification);
    }

    @Override
    public List<NotificationDto> getUserNotification(String username) {
        // 1. Entity List ကို Repository မှ ရယူပါ
        return repository.findByUserUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(n -> NotificationDto.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .type(n.getType().toString())
                        .isRead(n.isRead())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        repository.save(notification);
    }
}

