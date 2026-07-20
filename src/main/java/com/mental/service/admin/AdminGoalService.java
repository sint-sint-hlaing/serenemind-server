package com.mental.service.admin;

import com.mental.dto.admin.GoalStatisticDto;
import com.mental.dto.goal.UserGoal;

import java.util.List;

public interface AdminGoalService {
    List<UserGoal> getGoals();

    GoalStatisticDto getStatistics();
}
