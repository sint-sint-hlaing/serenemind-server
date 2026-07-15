package com.mental.dto.home;


import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

import java.time.DayOfWeek;

@Builder
public record WeeklyMoodResponse(
        DayOfWeek day,

        MoodType mood,

        Integer percentage

) {
}
