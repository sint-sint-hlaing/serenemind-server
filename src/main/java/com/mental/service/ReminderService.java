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

    // 👈 Schedulers သို့မဟုတ် Background Task တစ်ခုခုကနေ Reminder Missed ဖြစ်ကြောင်း လှမ်းခေါ်မည့် Method
    @Transactional
    public void triggerReminderMissed(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        User user = userRepository.findById(reminder.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reminder.getUserId()));

        // Noti ထဲတွင် ပြသမည့် စာသားပုံစံ ပြင်ဆင်ခြင်း
        String title = "Reminder missed";
        String message = "You missed your reminder \"" + reminder.getTitle() + "\" at " + reminder.getReminderTime();

        // NotificationService Helper Method ကို ခေါ်ယူခြင်း
        notificationService.createNotification(
                user,               // Noti လက်ခံမည့် အသုံးပြုသူ
                title,              // Title
                message,            // Message
                "REMINDER",         // Noti Icon ပြောင်းရန်အတွက် Type
                reminder.getId(),   // 👈 Target ID: Reminder ID ကို ပေးရပါမည်
                "REMINDER"          // 👈 Target Type: Frontend က Reminder Screen ကို သွားရန်
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
