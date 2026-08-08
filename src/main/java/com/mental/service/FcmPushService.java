package com.mental.service;

import com.google.firebase.messaging.*;
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

       List<DeviceToken> deviceTokens = deviceTokenRepository.findByUser(user);

        if (deviceTokens.isEmpty()) {
            log.info("No registered FCM tokens found for user: {}", user.getEmail());
            return;
        }

        List<String> tokens = deviceTokens.stream()
                .map(DeviceToken::getToken)
                .collect(Collectors.toList());

        AndroidConfig androidConfig = AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setSound("default")
                        .setChannelId("reminder_channel_v2")
                        .setPriority(AndroidNotification.Priority.HIGH)
                        .build())
                .build();

        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .build())
                .build();


        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(messageBody)
                        .build())
                .setAndroidConfig(androidConfig)
                .setApnsConfig(apnsConfig)
                .putData("targetId", targetId != null ? targetId.toString() : "")
                .putData("targetType", targetType != null ? targetType : "")
                .addAllTokens(tokens)
                .build();

        try {

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM multicast sent for user: {}. Success: {}, Failure: {}",
                    user.getEmail(), response.getSuccessCount(), response.getFailureCount());


            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse res = responses.get(i);

                    if (!res.isSuccessful() && res.getException() != null) {
                        MessagingErrorCode errorCode = res.getException().getMessagingErrorCode();

                        if (MessagingErrorCode.INVALID_ARGUMENT.equals(errorCode) ||
                                MessagingErrorCode.UNREGISTERED.equals(errorCode)) {

                            DeviceToken invalidTokenEntity = deviceTokens.get(i);
                            deviceTokenRepository.delete(invalidTokenEntity);
                            log.info("Automatically removed invalid token from DB: {}", invalidTokenEntity.getToken());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to execute FCM multicast notification", e);
        }
    }
}