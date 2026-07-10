package com.mental.dto.Reminder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReminderRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Repeat configuration is required")
    private String repeatType; // e.g., "Daily", "Mon, Wed, Fri"

    @NotNull(message = "Time is required")
    private LocalTime reminderTime; // e.g., 09:00 AM

    @NotNull(message = "Start date is required")
    private LocalDate startDate; // e.g., May 12, 2024

    private String reminderTone; // e.g., "Gentle Bell"

    private String note; // Optional additional details

    private boolean enabled = true; // For the toggle switch
}
