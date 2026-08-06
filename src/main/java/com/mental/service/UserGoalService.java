package com.mental.service;

import com.mental.dto.goal.GoalRequest;
import com.mental.dto.goal.GoalStatistics;
import com.mental.dto.goal.UserGoal;
import com.mental.model.entity.enums.GoalStatus;

import java.util.List;

public interface UserGoalService {

    // ===== CRUD =====
    UserGoal createGoal(String email, GoalRequest request);

    List<UserGoal> getUserGoals(String email);

    UserGoal completeGoal(Long id, String email);

    void deleteGoal(Long id, String email);

    void hardDeleteGoal(Long id, String email);

    // ===== Progress =====
    UserGoal updateProgress(Long id, String email);

    // ===== Status Management =====
    UserGoal pauseGoal(Long id, String email);

    UserGoal resumeGoal(Long id, String email);

    // ===== Filtering =====
    List<UserGoal> getActiveGoals(String email);

    List<UserGoal> getCompletedGoals(String email);

    List<UserGoal> getGoalsByStatus(String email, GoalStatus status);

    // ===== Statistics =====
    GoalStatistics getGoalStatistics(String email);

    // ===== Scheduled Jobs =====
    void checkExpiredGoals();

    // ===== UI Specific =====
    List<UserGoal> getGoalsForDashboard(String email);
}