package com.mental.dto.goal;

import com.mental.model.entity.enums.GoalStatus;
import lombok.Builder;

@Builder
public record UserGoal(

        Long id,

        String title,

        String description,

        int targetDays,

        int progress,

        GoalStatus status

) {}