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

    /**
     * အသုံးပြုသူ၏ Device အားလုံးဆီသို့ Notification အားလုံး (Reminder, Like, Comment, Post) ကို
     * စနစ်တကျ အသံပါဝင်ပြီး Performance အမြင့်ဆုံးစနစ်ဖြင့် ပို့ဆောင်ပေးမည်။
     */
    public void sendPushNotificationToUser(User user, String title, String messageBody, Long targetId, String targetType) {

        // ၁။ Database ထဲမှ အဆိုပါ User ၏ DeviceToken list ကို အရင်ဆွဲထုတ်ခြင်း
        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUser(user);

        if (deviceTokens.isEmpty()) {
            log.info("No registered FCM tokens found for user: {}", user.getEmail());
            return;
        }

        // Token စာသားသက်သက်ကို List အဖြစ်ပြောင်းလဲခြင်း
        List<String> tokens = deviceTokens.stream()
                .map(DeviceToken::getToken)
                .collect(Collectors.toList());

        // ၂။ Android အတွက် အသံနှင့် Heads-up Configuration သတ်မှတ်ခြင်း
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setSound("default")
                        .setChannelId("reminder_channel_v2") // Frontend Kotlin ဘက်က Channel ID နှင့် ကိုက်ညီရမည်
                        .setPriority(AndroidNotification.Priority.HIGH)
                        .build())
                .build();

        // ၃။ iOS အတွက် အသံမြည်ရန် သတ်မှတ်ခြင်း
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .build())
                .build();

        // ၄။ Multicast Message တည်ဆောက်ခြင်း (Null Safe ဖြစ်အောင် ပြင်ဆင်ထားပါသည်)
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
            // ၅။ Firebase ထံ Batch စနစ်ဖြင့် တစ်ကြိမ်တည်း ပို့လွှတ်ခြင်း
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM multicast sent for user: {}. Success: {}, Failure: {}",
                    user.getEmail(), response.getSuccessCount(), response.getFailureCount());

            // ၆။ အသုံးမဝင်တော့သော/သက်တမ်းကုန်သွားသော Token များကို DB ထဲမှ အလိုအလျောက် လိုက်လံဖျက်ဆီးခြင်း
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse res = responses.get(i);

                    if (!res.isSuccessful() && res.getException() != null) {
                        MessagingErrorCode errorCode = res.getException().getMessagingErrorCode();

                        // Error code သည် invalid သို့မဟုတ် unregistered ဖြစ်နေလျှင်
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