package com.mental.service;

import com.mental.model.entity.Reminder;
import com.mental.model.entity.User;
import com.mental.repository.ReminderRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderRepository reminderRepository;
    private final FcmPushService fcmPushService;
    private final UserRepository userRepository;

    // 0 * * * * * ဆိုသည်မှာ ၁ မိနစ်တိုင်း (00 စက္ကန့်ရောက်တိုင်း) အလုပ်လုပ်မည်
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkAndSendReminders() {
        // လက်ရှိအချိန် (မိနစ်အထိပဲယူမည်၊ စက္ကန့်တွေကို ဖြုတ်ထားမည်)
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = LocalDate.now();

        // အချိန်ရောက်နေသော၊ Active ဖြစ်နေသော Reminder များကို ရှာဖွေခြင်း
        List<Reminder> activeReminders = reminderRepository
                .findByEnabledTrueAndReminderTimeAndStartDateLessThanEqual(now, today);

        for (Reminder reminder : activeReminders) {
            User user = userRepository.findById(reminder.getUserId())
                    .orElse(null);

            if (user != null) {
                // FCM Push Notification ပို့ခြင်း
                fcmPushService.sendPushNotificationToUser(
                        user,
                        "Reminder: " + reminder.getTitle(),
                        "Time to: " + reminder.getNote(),
                        reminder.getId(),
                        "REMINDER"
                );
            }
        }
    }
}