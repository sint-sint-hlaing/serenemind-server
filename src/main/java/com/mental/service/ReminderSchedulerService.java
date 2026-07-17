package com.mental.service;

import com.mental.model.entity.Reminder;
import com.mental.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerService {

    private final ReminderRepository reminderRepository;
    private final ReminderService reminderService;

    /**
     * ၁ မိနစ်တစ်ခါ Background ကနေ Auto စစ်ပေးမယ့် Task ဖြစ်ပါတယ်။
     * fixedRate = 60000 ဆိုသည်မှာ ၆၀ စက္ကန့် (၁ မိနစ်) ကို ဆိုလိုသည်။
     */
    @Scheduled(fixedRate = 60000)
    public void checkPendingReminders() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Database ထဲကနေ enabled ဖြစ်နေပြီး အချိန်စေ့နေပြီဖြစ်တဲ့ (သို့) ကျော်နေတဲ့ Reminder တွေကို လိုက်ရှာမယ်
        // ⚠️ မှတ်ချက် - repository ထဲမှာ အချိန်စေ့/မစေ့ စစ်မယ့် Query Method တစ်ခု လိုပါလိမ့်မယ် (Step 2 တွင်ကြည့်ပါ)
        List<Reminder> pendingReminders = reminderRepository.findPendingReminders(now);

        log.info("Checking reminders at {}. Found {} pending reminders.", now, pendingReminders.size());

        for (Reminder reminder : pendingReminders) {
            try {
                // 2. သင့်ရဲ့ လက်ရှိ ကုဒ်ထဲက Noti ပို့မယ့် Method ကို လှမ်းခေါ်လိုက်ပါတယ်
                reminderService.triggerReminderMissed(reminder.getId());

                // 3. Noti တစ်ခါ ပို့ပြီးရင် ထပ်ခါထပ်ခါ မသွားအောင် ခေတ္တပိတ်ထားတာမျိုး (သို့) Repeat Type အလိုက် အချိန်ပြင်တာမျိုး လုပ်ရပါမယ်
                // အောက်ပါကုဒ်သည် Noti တစ်ခါပြပြီးရင် ထပ်မပြအောင် ပိတ်လိုက်သည့် ပုံစံဖြစ်သည်
                reminder.setEnabled(false);
                reminderRepository.save(reminder);

            } catch (Exception e) {
                log.error("Error triggering reminder ID: " + reminder.getId(), e);
            }
        }
    }
}