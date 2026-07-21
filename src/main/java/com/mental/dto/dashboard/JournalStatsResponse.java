package com.mental.dto.dashboard;

import lombok.Builder;

@Builder
public record JournalStatsResponse(

        long totalJournals,

        long todayJournals,

        long flaggedJournals,

        double growthPercentage

) {}
