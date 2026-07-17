package com.mental.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.mental.model.entity.DeviceToken;
import com.mental.model.entity.User;
import com.mental.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {

    private final DeviceTokenRepository deviceTokenRepository;

    public void sendPushNotificationToUser(User user, String title, String messageBody, Long targetId, String targetType) {

        // 👈 User entity ကနေ မယူတော့ဘဲ Repository ကနေ တိုက်ရိုက် ဆွဲထုတ်ခြင်း
        List<String> tokens = deviceTokenRepository.findByUser(user).stream()
                .map(DeviceToken::getToken)
                .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            log.info("No registered FCM tokens found for user: {}", user.getId());
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(messageBody)
                        .build())
                .putData("targetId", String.valueOf(targetId))
                .putData("targetType", targetType)
                .addAllTokens(tokens)
                .build();

        try {
            FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("Successfully sent FCM reminder to user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send FCM notification", e);
        }
    }
}