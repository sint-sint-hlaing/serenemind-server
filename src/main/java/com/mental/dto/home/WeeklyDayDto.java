package com.mental.dto.home;

import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

import java.time.DayOfWeek;

@Builder
public record WeeklyDayDto(
        DayOfWeek day,
        String dayLabel,          // e.g., "Mon"
        Integer score,            // Bar height value (0-100)
        MoodType mood,
        boolean hasData
) {}