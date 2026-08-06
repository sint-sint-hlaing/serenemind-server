package com.mental.service;

import com.mental.dto.Reminder.ReminderRequest;
import com.mental.dto.Reminder.ReminderResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.Reminder;
import com.mental.model.entity.User;
import com.mental.repository.ReminderRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;

    @Transactional(readOnly = true)
    public List<ReminderResponse> getRemindersByUserId(UserPrincipal userPrincipal) {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);

        return reminderRepository.findByUserIdSorted(userPrincipal.getId(), today, nowTime)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReminderResponse createReminder(UserPrincipal userPrincipal, ReminderRequest request) {

        LocalDate finalStartDate = request.getStartDate();
        LocalTime nowTime = LocalTime.now();
        LocalDate today = LocalDate.now();

        // 💡 အကယ်၍ သတ်မှတ်ထားသောရက်က ဒီနေ့ဖြစ်ပြီး၊ အချိန်က လက်ရှိအချိန်ထက် ကျော်လွန်နေခဲ့လျှင်
        if (finalStartDate.equals(today) && request.getReminderTime().isBefore(nowTime)) {
            String repeatType = request.getRepeatType() != null ? request.getRepeatType().toUpperCase() : "ONCE";

            switch (repeatType) {
                case "ONCE":
                case "DAILY":
                    // ONCE ကော DAILY ကော အချိန်ကျော်လျှင် မနက်ဖြန် (Tomorrow) သို့ ရွှေ့မည်
                    finalStartDate = today.plusDays(1);
                    break;

                case "WEEKLY":
                    finalStartDate = today.plusWeeks(1);
                    break;

                case "MONTHLY":
                    finalStartDate = today.plusMonths(1);
                    break;

                case "CUSTOM_DAYS":
                    if (request.getRepeatDays() != null && !request.getRepeatDays().isEmpty()) {
                        LocalDate nextDate = today;
                        boolean foundNextMatch = false;

                        for (int i = 1; i <= 7; i++) {
                            nextDate = nextDate.plusDays(1);
                            String dayOfWeekName = nextDate.getDayOfWeek().name();

                            if (request.getRepeatDays().toUpperCase().contains(dayOfWeekName)) {
                                finalStartDate = nextDate;
                                foundNextMatch = true;
                                break;
                            }
                        }
                        if (!foundNextMatch) {
                            finalStartDate = today.plusDays(1);
                        }
                    } else {
                        finalStartDate = today.plusDays(1);
                    }
                    break;
            }
        }

        Reminder reminder = Reminder.builder()
                .userId(userPrincipal.getId())
                .title(request.getTitle())
                .repeatType(request.getRepeatType())
                .repeatDays(request.getRepeatDays())
                .reminderTime(request.getReminderTime())
                .startDate(finalStartDate) // 👈 တွက်ချက်ပြီးသား ရက်စွဲအသစ်ကို ထည့်သွင်းမည်
                .reminderTone(request.getReminderTone())
                .note(request.getNote())
                .enabled(request.isEnabled())
                .build();

        Reminder savedReminder = reminderRepository.save(reminder);
        return convertToResponse(savedReminder);
    }

    @Transactional
    public ReminderResponse toggleReminderStatus(Long id, UserPrincipal userPrincipal) {
        Reminder reminder = reminderRepository.findByIdAndUserId(id, userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found or unauthorized"));

        reminder.setEnabled(!reminder.isEnabled());
        return convertToResponse(reminder);
    }

    @Transactional
    public void deleteReminder(Long id, UserPrincipal userPrincipal) {
        Reminder reminder = reminderRepository.findByIdAndUserId(id, userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found or unauthorized"));

        reminderRepository.delete(reminder);
    }

//    @Async
// @Async <-- ❌ Noti ၂ စောင်ပွားခြင်းမှ ကာကွယ်ရန် ဤလိုင်းကို လုံးဝ ဖြတ်ပစ်ပါ
@Transactional
public void triggerReminderAlert(Long reminderId) {
    Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

    User user = userRepository.findById(reminder.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reminder.getUserId()));

    String title = "Reminder Alert";
    String message = "It's time for your reminder: \"" + reminder.getTitle() + "\"";

    // DB ထဲ Notification Record ထည့်ခြင်း
    notificationService.createNotification(user, title, message, "REMINDER", reminder.getId(), "REMINDER");

    // FCM Push Notification ပါ တစ်ပါတည်း တိုက်ရိုက်ပို့ခြင်း (အပေါ်က @Async ဖြုတ်ထား၍ ဤနေရာတွင် စိတ်ချလက်ချ ခေါ်နိုင်ပါပြီ)
//    fcmPushService.sendPushNotificationToUser(user, title, message, reminder.getId(), "REMINDER");
}

    // Helper method to convert Entity to Response DTO
    private ReminderResponse convertToResponse(Reminder reminder) {
        ReminderResponse response = new ReminderResponse();
        response.setId(reminder.getId());
        response.setTitle(reminder.getTitle());
        response.setRepeatType(reminder.getRepeatType());
        response.setRepeatDays(reminder.getRepeatDays()); // 👈 ၂။ App ဘက်ကို Response ပြန်ပေးနိုင်ရန် ထည့်ပါ
        response.setReminderTime(reminder.getReminderTime());
        response.setStartDate(reminder.getStartDate());
        response.setReminderTone(reminder.getReminderTone());
        response.setNote(reminder.getNote());
        response.setEnabled(reminder.isEnabled());
        return response;
    }
}