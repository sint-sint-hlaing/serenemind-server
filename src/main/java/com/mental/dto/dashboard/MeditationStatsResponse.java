package com.mental.dto.dashboard;

import lombok.Builder;

@Builder
public record MeditationStatsResponse(

        long totalSessions,

        long completedSessions,

        long activeUsers,

        double completionRate

) {}