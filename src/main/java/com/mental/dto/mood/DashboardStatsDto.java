package com.mental.dto.mood;

import java.util.List;

public record DashboardStatsDto(
        long totalUsers,
        long activeUsers,
        long totalMoodEntries,
        long totalJournalEntries,
        long totalMeditationSessions,
        List<MoodDistributionDto> moodDistributions
) {
    public static DashboardStatsDto of(long totalUsers, long activeUsers, long totalMoodEntries,
                                       long totalJournalEntries, long totalMeditationSessions,
                                       List<MoodDistributionDto> moodDistributions) {
        return new DashboardStatsDto(totalUsers, activeUsers, totalMoodEntries,
                totalJournalEntries, totalMeditationSessions, moodDistributions);
    }
}


