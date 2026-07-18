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
    @Transactional
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
                // ၁။ Noti ပို့မယ်
                reminderService.triggerReminderAlert(reminder.getId());

                // ၂။ Repeat Type အလိုက် Logic စစ်မယ်
                String repeatType = reminder.getRepeatType();

                if (repeatType == null || "ONCE".equalsIgnoreCase(repeatType)) {
                    reminder.setEnabled(false);
                } else {
                    switch (repeatType.toUpperCase()) {
                        case "DAILY":
                            reminder.setStartDate(reminder.getStartDate().plusDays(1));
                            break;

                        case "CUSTOM_DAYS": // 👈 ရက်အလိုက် ထပ်ခါတလဲလဲ လုပ်မည့် Logic အသစ်
                            if (reminder.getRepeatDays() != null && !reminder.getRepeatDays().isEmpty()) {
                                LocalDate nextDate = reminder.getStartDate();
                                boolean foundNextMatch = false;

                                // လာမယ့် ၇ ရက်အတွင်း အသုံးပြုသူရွေးထားတဲ့ နေ့နဲ့ ကိုက်ညီမယ့်ရက်ကို လိုက်ရှာမယ်
                                for (int i = 1; i <= 7; i++) {
                                    nextDate = nextDate.plusDays(1);
                                    String dayOfWeekName = nextDate.getDayOfWeek().name(); // ဥပမာ - "MONDAY"

                                    // DB ထဲက သိမ်းထားတဲ့ စာသား (ဥပမာ- "MONDAY,WEDNESDAY") ထဲမှာ ပါဝင်လား စစ်တယ်
                                    if (reminder.getRepeatDays().toUpperCase().contains(dayOfWeekName)) {
                                        reminder.setStartDate(nextDate);
                                        foundNextMatch = true;
                                        break;
                                    }
                                }

                                // ၇ ရက်လုံး ရှာလို့မတွေ့ရင် (ဥပမာ စာသားမှားထည့်ထားရင်) နောက်ထပ်မမြည်အောင် ပိတ်မယ်
                                if (!foundNextMatch) {
                                    log.warn("No matching day found in repeat days '{}' for reminder ID: {}. Disabling.", reminder.getRepeatDays(), reminder.getId());
                                    reminder.setEnabled(false);
                                }
                            } else {
                                // Repeat Days မရှိရင် ပိတ်ပစ်မယ်
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

            } catch (Exception e) {
                log.error("Error triggering reminder ID: " + reminder.getId(), e);
            }
        }

        reminderRepository.saveAll(pendingReminders);
    }
}