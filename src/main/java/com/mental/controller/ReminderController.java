package com.mental.controller;

import com.mental.dto.Reminder.ReminderRequest;
import com.mental.dto.Reminder.ReminderResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    // UI - Reminders List Screen (အသုံးပြုသူ၏ Reminders အားလုံးကို ပြရန်)
    @GetMapping
    public ResponseEntity<List<ReminderResponse>> getUserReminders(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ReminderResponse> reminders = reminderService.getRemindersByUserId(userPrincipal);
        return ResponseEntity.ok(reminders);
    }

    // UI - New Reminder Screen -> Save (နှိပ်ပြီး Reminder အသစ် သိမ်းဆည်းရန်)
    @PostMapping
    public ResponseEntity<ReminderResponse> createReminder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ReminderRequest request) {

        ReminderResponse response = reminderService.createReminder(userPrincipal, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // UI - Reminders Screen Toggle (Reminder တစ်ခုချင်းစီကို ဖွင့်/ပိတ် Toggle လုပ်ရန်)
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ReminderResponse> toggleReminderStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        ReminderResponse response = reminderService.toggleReminderStatus(id, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // Optional: UI - Reminder တစ်ခုကို ဖျက်ချင်လျှင် သို့မဟုတ် ပြန်ပြင်ချင်လျှင် သုံးနိုင်ရန်
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        reminderService.deleteReminder(id, userPrincipal);
        return ResponseEntity.noContent().build();
    }
}
