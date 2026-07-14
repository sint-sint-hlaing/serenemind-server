package com.mental.service;

import com.mental.dto.GoalRequest;
import com.mental.dto.goal.UserGoal;

import java.util.List;

public interface UserGoalService {


    UserGoal createGoal(String email, GoalRequest request);

    List<UserGoal> getUserGoals(String email);

    UserGoal updateProgress(Long id);

    UserGoal completeGoal(Long id);

    void deleteGoal(Long id);
}

