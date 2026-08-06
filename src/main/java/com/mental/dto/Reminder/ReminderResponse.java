package com.mental.dto.Reminder;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReminderResponse {
    private Long id;
    private String title;
    private String repeatType;
    private String repeatDays;
    private LocalTime reminderTime;
    private LocalDate startDate;
    private String reminderTone;
    private String note;
    private boolean enabled;
}
