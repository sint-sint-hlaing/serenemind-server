package com.mental.service;

import com.mental.model.entity.Reminder;
import com.mental.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerService {

    private final ReminderRepository reminderRepository;
    private final ReminderService reminderService;

    @Scheduled(fixedRate = 60000)
    @Transactional // 👈 Transaction စတင်သည်
    public void checkPendingReminders() {
        LocalDateTime now = LocalDateTime.now();

        List<Reminder> pendingReminders = reminderRepository.findPendingReminders(
                now.toLocalDate(),
                now.toLocalTime()
        );

        log.info("Checking reminders at {}. Found {} pending reminders.", now, pendingReminders.size());

        if (pendingReminders.isEmpty()) {
            return;
        }

        for (Reminder reminder : pendingReminders) {
            try {
                // 🎯 ၁။ ၎င်း Reminder အတွက် Repeat Type အလိုက် ရက်စွဲ/အခြေအနေကို ချက်ချင်း အရင်ပြင်ဆင်ပါမည်
                String repeatType = reminder.getRepeatType();

                if (repeatType == null || "ONCE".equalsIgnoreCase(repeatType)) {
                    reminder.setEnabled(false);
                } else {
                    switch (repeatType.toUpperCase()) {
                        case "DAILY":
                            reminder.setStartDate(reminder.getStartDate().plusDays(1));
                            break;

                        case "CUSTOM_DAYS":
                            if (reminder.getRepeatDays() != null && !reminder.getRepeatDays().isEmpty()) {
                                LocalDate nextDate = reminder.getStartDate();
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
                                    log.warn("No matching day found in repeat days '{}' for reminder ID: {}. Disabling.", reminder.getRepeatDays(), reminder.getId());
                                    reminder.setEnabled(false);
                                }
                            } else {
                                reminder.setEnabled(false);
                            }
                            break;

                        case "WEEKLY":
                            reminder.setStartDate(reminder.getStartDate().plusWeeks(1));
                            break;
                        case "MONTHLY":
                            reminder.setStartDate(reminder.getStartDate().plusMonths(1));
                            break;
                        default:
                            reminder.setEnabled(false);
                            break;
                    }
                }

                // 🎯 ၂။ 👈 အရေးကြီးဆုံးနေရာ - Noti မပို့ခင် DB ထဲကို အပြောင်းအလဲကို ချက်ချင်း ရိုက်ထည့်လိုက်ပါသည်
                // ၎င်းကြောင့် အခြား Async Thread သို့မဟုတ် Scheduler က ထပ်မံဆွဲထုတ်၍ မရတော့ပါ။
                reminderRepository.saveAndFlush(reminder);

                // 🎯 ၃။ DB ထဲတွင် အခြေအနေ ပိတ်/ပြောင်း ပြီးမှသာ Noti ကို စိတ်ချလက်ချ လှမ်းပို့ပါမည်
                reminderService.triggerReminderAlert(reminder.getId());

            } catch (Exception e) {
                log.error("Error triggering reminder ID: " + reminder.getId(), e);
            }
        }

        // ကွင်းပိတ်အောက်ခြေက saveAll လိုင်းကို ဖြတ်ပစ်နိုင်ပါပြီ (အပေါ်မှာ တစ်ခုချင်းစီ saveAndFlush လုပ်ခဲ့ပြီးဖြစ်၍)
    }
}