// UserGoalService.java
package com.mental.service;

import com.mental.dto.goal.*;
import com.mental.model.entity.enums.GoalStatus;
import jakarta.validation.Valid;

import java.util.List;

public interface UserGoalService {

    // ===== CRUD =====
    GoalResponse createGoal(String email, GoalRequest request);
    List<GoalResponse> getUserGoals(String email);
    GoalResponse completeGoal(Long id, String email);
    void deleteGoal(Long id, String email);
    void hardDeleteGoal(Long id, String email);

    // ===== Progress =====
    GoalResponse updateProgress(Long id, String email);

    // ===== Status Management =====
    GoalResponse pauseGoal(Long id, String email);
    GoalResponse resumeGoal(Long id, String email);

    // ===== Filtering =====
    List<GoalResponse> getActiveGoals(String email);
    List<GoalResponse> getCompletedGoals(String email);
    List<GoalResponse> getGoalsByStatus(String email, GoalStatus status);

    // ===== Statistics =====
    GoalStatistics getGoalStatistics(String email);

    // ===== Scheduled Jobs =====
    void checkExpiredGoals();

    // ===== UI Specific =====
    List<GoalResponse> getGoalsForDashboard(String email);

    // ===== Note Management =====
    GoalNoteDTO addNote(Long goalId, String email, String content);
    void deleteNote(Long noteId, String email);
    GoalNoteDTO updateNote(Long noteId, String email, String content);
    List<GoalNoteDTO> getNotesByGoal(Long goalId, String email);

    GoalResponse getGoalById(Long id, String email);

    GoalResponse updateGoal(Long id, String email, @Valid GoalRequest request);

    GoalResponse updateProgress(Long id, String email, ProgressUpdateRequest request);
}