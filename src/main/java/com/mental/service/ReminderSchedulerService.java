package com.mental.service;

import com.mental.model.entity.Reminder;
import com.mental.model.entity.User;
import com.mental.repository.ReminderRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService; // 💡 Noti ပွားခြင်းမှ ကာကွယ်ရန် ဤနေရာတွင် NotificationService ကို သုံးပါမည်

    @Autowired
    @Lazy
    private ReminderSchedulerService self;

    // 💡 00 စက္ကန့်ရောက်တိုင်း ၁ မိနစ်တစ်ကြိမ် တိတိကျကျ အလုပ်လုပ်ရန် Cron စနစ်သို့ ပြောင်းလဲထားပါသည်
    @Scheduled(cron = "0 * * * * *")
    public void checkPendingReminders() {
        // စက္ကန့်နှင့် မီလီစက္ကန့်ကို 00 အဖြစ် ဖြတ်ချလိုက်ခြင်းဖြင့် အချိန်ကွက်တိ ကိုက်ညီစေပါသည်
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        // ရောက်ရှိနေသော Pending Reminder များကို ဆွဲထုတ်ခြင်း (Repository Query က `=` စနစ် ဖြစ်ရပါမည်)
        List<Reminder> pendingReminders = reminderRepository.findPendingReminders(today, nowTime);

        log.info("Checking reminders at {}. Found {} pending reminders.", now, pendingReminders.size());

        if (pendingReminders.isEmpty()) {
            return;
        }

        for (Reminder reminder : pendingReminders) {
            try {
                // ၁။ Status/ရက်စွဲကို အရင် Update လုပ်ပြီး သီးသန့် Transaction ဖြင့် Commit အရင်ချမည်
                self.updateReminderStatusInNewTransaction(reminder.getId());

                // ၂။ သုံးစွဲသူကို ရှာဖွေပြီး Noti ပို့မည်
                User user = userRepository.findById(reminder.getUserId()).orElse(null);

                if (user != null) {
                    // 💡 NotificationService မှတစ်ဆင့် ခေါ်ယူသဖြင့် DB ထဲ Noti ဝင်ပြီး FCM ပါ တစ်ကြိမ်တည်း ကွက်တိ ပို့ပေးသွားမည် ဖြစ်သည်
                    notificationService.createNotification(
                            user,
                            "Reminder: " + reminder.getTitle(),
                            "Time to: " + (reminder.getNote() != null ? reminder.getNote() : reminder.getTitle()),
                            "REMINDER",
                            reminder.getId(),
                            "REMINDER"
                    );
                }

            } catch (Exception e) {
                log.error("Error triggering reminder ID: " + reminder.getId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateReminderStatusInNewTransaction(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found: " + reminderId));

        String repeatType = reminder.getRepeatType();
        LocalDate today = LocalDate.now();

        if (repeatType == null || "ONCE".equalsIgnoreCase(repeatType)) {
            // ONCE သမားဖြစ်လျှင် Noti တက်ပြီးပါက အလိုအလျောက် ပိတ်ပစ်မည်
            reminder.setEnabled(false);
        } else {
            switch (repeatType.toUpperCase()) {
                case "DAILY":
                    LocalDate baseDailyDate = reminder.getStartDate().isBefore(today) ? today : reminder.getStartDate();
                    reminder.setStartDate(baseDailyDate.plusDays(1));
                    break;

                case "CUSTOM_DAYS":
                    if (reminder.getRepeatDays() != null && !reminder.getRepeatDays().isEmpty()) {
                        LocalDate nextDate = today;
                        boolean foundNextMatch = false;

                        for (int i = 1; i <= 7; i++) {
                            nextDate = nextDate.plusDays(1);
                            String dayOfWeekName = nextDate.getDayOfWeek().name();

                            if (reminder.getRepeatDays().toUpperCase().contains(dayOfWeekName)) {
                                reminder.setStartDate(nextDate);
                                foundNextMatch = true;
                                break;
                            }
                        }

                        if (!foundNextMatch) {
                            reminder.setEnabled(false);
                        }
                    } else {
                        reminder.setEnabled(false);
                    }
                    break;

                case "WEEKLY":
                    LocalDate baseWeeklyDate = reminder.getStartDate().isBefore(today) ? today : reminder.getStartDate();
                    reminder.setStartDate(baseWeeklyDate.plusWeeks(1));
                    break;

                case "MONTHLY":
                    LocalDate baseMonthlyDate = reminder.getStartDate().isBefore(today) ? today : reminder.getStartDate();
                    reminder.setStartDate(baseMonthlyDate.plusMonths(1));
                    break;

                default:
                    reminder.setEnabled(false);
                    break;
            }
        }

        reminderRepository.saveAndFlush(reminder);
    }
}