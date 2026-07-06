package com.mental.dto.mood;

public record MoodDistributionDto(
        String moodType,
        long count,
        double percentage
) {
}