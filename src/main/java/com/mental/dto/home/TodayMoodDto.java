package com.mental.dto.home;

import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

@Builder
public record TodayMoodDto(
        boolean hasLogged,
        MoodType mood,
        Integer percentage,       // e.g., 80
        String message            // e.g., "Great! Keep shining ✨"
) {}