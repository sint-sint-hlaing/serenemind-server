package com.mental.dto.dashboard;

import lombok.Builder;

@Builder
public record MoodStatsResponse(

        long totalEntries,

        String mostCommonMood,

        double averageScore

) {}