package com.mental.service.admin;

import com.mental.dto.admin.GoalStatisticDto;
import com.mental.dto.goal.GoalResponse;
import com.mental.dto.goal.UserGoal;

import java.util.List;

public interface AdminGoalService {
    List<GoalResponse> getGoals();

    GoalStatisticDto getStatistics();
}
