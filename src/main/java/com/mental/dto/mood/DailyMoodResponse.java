package com.mental.dto.mood;

import lombok.Builder;

@Builder
public record DailyMoodResponse(
        String date,
        String mood,
        int intensity,
        String note
) {
}
