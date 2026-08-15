package com.mental.dto.home;

import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

import java.util.List;

@Builder
public record DashboardResponse(
        String greeting,          // e.g., "Good morning, Aye!"
        String formattedDate,     // e.g., "May 12, 2024"
        TodayMoodDto todayMood,
        List<WeeklyDayDto> weeklyOverview
) {}