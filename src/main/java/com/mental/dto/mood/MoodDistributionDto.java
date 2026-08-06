package com.mental.dto.mood;

import com.mental.model.entity.enums.MoodType;

public record MoodDistributionDto(
        MoodType mood,
        long count,
        double percentage
) {
}