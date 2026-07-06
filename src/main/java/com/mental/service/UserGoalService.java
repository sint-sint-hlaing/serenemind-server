package com.mental.service;

import com.mental.dto.GoalRequest;
import com.mental.model.entity.User;
import com.mental.model.entity.UserGoal;
import org.springframework.stereotype.Service;

import java.util.List;

public interface UserGoalService {
    UserGoal createGoal(String username, GoalRequest request);
    UserGoal updateProgress(Long id);
    List<UserGoal> getUserGoals(String username);
    UserGoal completeGoal(Long id);
    void deleteGoal(Long id);
    }

