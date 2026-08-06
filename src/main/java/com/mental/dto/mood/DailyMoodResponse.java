package com.mental.dto.mood;

import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DailyMoodResponse(
        LocalDate date,
        MoodType mood,
        int intensity,
        Integer score,

        String note
) {
}
