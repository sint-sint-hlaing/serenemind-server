package com.mental.dto.home;

import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

@Builder
public record TodayMoodResponse(

        MoodType mood,

        Integer percentage,

        String message

) {
}
