package com.mental.dto;

import java.util.List;

public record DashboardResponse(
        String username,
        String currentMood,
        int moodPercentage,
        List<WeeklyData> weeklyDataList,
        List<ActionItem> quickActions


) {
}
