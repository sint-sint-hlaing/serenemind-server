package com.mental.dto.admin;


import lombok.Builder;


@Builder
public record GoalStatisticDto(

        Long totalGoals,

        Long completedGoals,

        Long inProgressGoals,

        Long failedGoals,

        Double completionRate

) {

}