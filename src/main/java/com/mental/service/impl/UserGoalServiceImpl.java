package com.mental.service.impl;

import com.mental.dto.GoalRequest;
import com.mental.exception.UserNotFoundException;
import com.mental.model.entity.*;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.repository.UserGoalRepository;
import com.mental.repository.UserStreakRepository;
import com.mental.repository.UserRepository;
import com.mental.service.UserGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static java.rmi.server.LogStream.log;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserGoalServiceImpl implements UserGoalService {
    private final UserGoalRepository goalRepository;
    private final UserStreakRepository streakRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserGoal createGoal(String email, GoalRequest request) {
        User user = userRepository.findByEmail( email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserGoal goal = new UserGoal();
        goal.setUser(user);
        goal.setTitle(request.title());
        goal.setDescription(request.description());
        goal.setTargetDays(request.targetDays());
        goal.setProgress(0);
        goal.setStatus(GoalStatus.ACTIVE);
        return goalRepository.save(goal);
    }

    @Transactional
    public UserGoal updateProgress(Long id) {
        UserGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Retrieve or initialize the user's streak
        UserStreak streak = streakRepository.findByUser(goal.getUser())
                .orElseGet(() -> {
                    UserStreak newStreak = new UserStreak();
                    newStreak.setUser(goal.getUser());
                    newStreak.setStreakCount(0);
                    return streakRepository.save(newStreak);
                });

        LocalDate today = LocalDate.now();

        // 1. Progress and Streak Logic
        // Only allow progress update once per day
        if (streak.getLastCompleted() == null || !streak.getLastCompleted().isEqual(today)) {

            // Increment progress
            if (goal.getProgress() < goal.getTargetDays()) {
                goal.setProgress(goal.getProgress() + 1);
            }

            // Streak calculation logic
            if (streak.getLastCompleted() != null && streak.getLastCompleted().isEqual(today.minusDays(1))) {
                streak.setStreakCount(streak.getStreakCount() + 1);
            } else if (streak.getLastCompleted() == null || streak.getLastCompleted().isBefore(today.minusDays(1))) {
                streak.setStreakCount(1);
            }

            streak.setLastCompleted(today);
            streakRepository.save(streak);
        }

        // 2. Goal Completion Status
        if (goal.getProgress() >= goal.getTargetDays()) {
            goal.setStatus(GoalStatus.COMPLETED);
        }

        return goalRepository.save(goal);
    }
    @Override
    public List<UserGoal> getUserGoals(String username) {
        return goalRepository.findByUserUsername(username);
    }

    @Override
    @Transactional
    public UserGoal completeGoal(Long id) {
        UserGoal goal = goalRepository.findById(id).orElseThrow();
        goal.setStatus(GoalStatus.COMPLETED);
        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    public void deleteGoal(Long id) {
        goalRepository.deleteById(id);
    }
}