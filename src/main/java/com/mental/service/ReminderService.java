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
        return reminderRepository.findByUserIdSorted(userPrincipal.getId())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReminderResponse createReminder(UserPrincipal userPrincipal, ReminderRequest request) {
        Reminder reminder = Reminder.builder()
                .userId(userPrincipal.getId())
                .title(request.getTitle())
                .repeatType(request.getRepeatType())
                .repeatDays(request.getRepeatDays()) // 👈 ၁။ ရက်အလိုက် သိမ်းဆည်းနိုင်ရန် ဤနေရာတွင် ထည့်ပါ
                .reminderTime(request.getReminderTime())
                .startDate(request.getStartDate())
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

    @Async
    @Transactional
    public void triggerReminderAlert(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        User user = userRepository.findById(reminder.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reminder.getUserId()));

        String title = "Reminder Alert";
        String message = "It's time for your reminder: \"" + reminder.getTitle() + "\"";

        // ၁။ Database ထဲ Noti Record ဝင်အောင် ဆောက်ပါသည် (ဒီကောင်ကိုပဲ ချန်ထားပါ)
        notificationService.createNotification(user, title, message, "REMINDER", reminder.getId(), "REMINDER");

        // ❌ ၂။ 👈 ဤလိုင်းကို လုံးဝ ဖြတ်ပစ်ပါ သို့မဟုတ် Comment ပိတ်ပါ (ဒါမှ Noti ၂ စောင် မပွားတော့မှာပါ)
        // fcmPushService.sendPushNotificationToUser(user, title, message, reminder.getId(), "REMINDER");
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