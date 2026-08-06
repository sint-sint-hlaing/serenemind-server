package com.mental.dto.mood;

import com.mental.model.entity.enums.MoodType;

public record MoodRequest(
        MoodType mood,
        int intensity,
        Integer score,
        String note
) {
}
