package com.mental.service.impl;

import com.mental.dto.GoalRequest;
import com.mental.model.entity.*;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.repository.UserGoalRepository;
import com.mental.repository.UserStreakRepository;
import com.mental.repository.UserRepository;
import com.mental.service.UserGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserGoalServiceImpl implements UserGoalService {
    private final UserGoalRepository goalRepository;
    private final UserStreakRepository streakRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserGoal createGoal(String username, GoalRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserGoal goal = new UserGoal();
        goal.setUser(user);
        goal.setTitle(request.title());
        goal.setDescription(request.description());
        goal.setTargetDays(request.targetDays());
        goal.setProgress(0);
        goal.setStatus(GoalStatus.ACTIVE);
        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    public UserGoal updateProgress(Long id) {
        UserGoal goal = goalRepository.findById(id).orElseThrow();
        UserStreak streak = streakRepository.findByUser(goal.getUser());
        LocalDate today = LocalDate.now();

        // 1. Progress တိုးခြင်း (ဒီနေ့အတွက် တစ်ကြိမ်သာ တိုးခွင့်ပေးခြင်း)
        if (streak.getLastCompleted() == null || !streak.getLastCompleted().isEqual(today)) {

            // Progress Update
            if (goal.getProgress() < goal.getTargetDays()) {
                goal.setProgress(goal.getProgress() + 1);
            }

            // 2. Streak Logic
            if (streak.getLastCompleted() != null && streak.getLastCompleted().isEqual(today.minusDays(1))) {
                streak.setStreakCount(streak.getStreakCount() + 1); // ရက်ဆက်ရင် +1
            } else if (streak.getLastCompleted() == null || streak.getLastCompleted().isBefore(today.minusDays(1))) {
                streak.setStreakCount(1); // အဆက်ပြတ်သွားရင် 1 ပြန်စ
            }

            // Update streak and completion status
            streak.setLastCompleted(today);
            streakRepository.save(streak);
        }

        // 3. Goal Completion Status
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