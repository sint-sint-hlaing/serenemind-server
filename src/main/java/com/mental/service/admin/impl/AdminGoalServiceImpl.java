package com.mental.service.admin.impl;

import com.mental.dto.admin.GoalStatisticDto;
import com.mental.dto.goal.UserGoal;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.UserGoalMapper;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.repository.UserGoalRepository;
import com.mental.service.admin.AdminGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGoalServiceImpl implements AdminGoalService {

    private final UserGoalRepository goalRepository;
    private final UserGoalMapper goalMapper;

    @Override
    public List<UserGoal> getGoals() {
        log.info("Fetching all goals");

        return goalRepository.findAll()
                .stream()
                .map(goalMapper::toDto)
                .toList();
    }


    @Override
    @Cacheable(value = "goalStatistics", key = "'goalStats'")
    public GoalStatisticDto getStatistics() {
        log.info("Calculating goal statistics");

        long total = goalRepository.count();
        long completed = goalRepository.countByStatus(GoalStatus.COMPLETED);
        long inProgress = goalRepository.countByStatus(GoalStatus.ACTIVE);
        long failed = goalRepository.countByStatus(GoalStatus.PAUSED);

        double completionRate = calculateCompletionRate(total, completed);

        return GoalStatisticDto.builder()
                .totalGoals(total)
                .completedGoals(completed)
                .inProgressGoals(inProgress)
                .failedGoals(failed)
                .completionRate(completionRate)
                .build();
    }

    private double calculateCompletionRate(long total, long completed) {
        if (total == 0) {
            return 0.0;
        }
        return (completed * 100.0) / total;
    }
}