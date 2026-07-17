package com.mental.service;

import com.mental.dto.Reminder.ReminderRequest;
import com.mental.dto.Reminder.ReminderResponse;
import com.mental.exception.ResourceNotFoundException; // Replace with your custom exception
import com.mental.model.entity.Reminder;
import com.mental.model.entity.User;
import com.mental.repository.ReminderRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public void triggerReminderMissed(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        User user = userRepository.findById(reminder.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reminder.getUserId()));

        String title = "Reminder missed";
        String message = "You missed your reminder \"" + reminder.getTitle() + "\" at " + reminder.getReminderTime();

        // ၁။ တွင်းဖြစ် Database ထဲ Noti သိမ်းခြင်း (ဒီအတိုင်းထားပါ)
        notificationService.createNotification(
                user, title, message, "REMINDER", reminder.getId(), "REMINDER"
        );

        // ၂။ 👈 Mobile ဖုန်းဆီ Real-time Alert တက်လာအောင် FCM Push Notification ပို့ခြင်း
        fcmPushService.sendPushNotificationToUser(
                user, title, message, reminder.getId(), "REMINDER"
        );
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> getRemindersByUserId(UserPrincipal userPrincipal) {
        return reminderRepository.findByUserId(userPrincipal.getId())
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

        // Toggle the boolean value
        reminder.setEnabled(!reminder.isEnabled());

        return convertToResponse(reminder);
    }

    @Transactional
    public void deleteReminder(Long id, UserPrincipal userPrincipal) {
        Reminder reminder = reminderRepository.findByIdAndUserId(id, userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found or unauthorized"));

        reminderRepository.delete(reminder);
    }

    // Helper method to convert Entity to Response DTO
    private ReminderResponse convertToResponse(Reminder reminder) {
        ReminderResponse response = new ReminderResponse();
        response.setId(reminder.getId());
        response.setTitle(reminder.getTitle());
        response.setRepeatType(reminder.getRepeatType());
        response.setReminderTime(reminder.getReminderTime());
        response.setStartDate(reminder.getStartDate());
        response.setReminderTone(reminder.getReminderTone());
        response.setNote(reminder.getNote());
        response.setEnabled(reminder.isEnabled());
        return response;
    }
}
