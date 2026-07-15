package com.mental.dto.home;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record DashboardResponse(

        String username,

        String greeting,

        LocalDate date,

        TodayMoodResponse todayMood,

        List<WeeklyMoodResponse> weeklyOverview,

        List<QuickActionResponse> quickActions,

        Integer currentStreak,

        Boolean isNewBest

) {

}