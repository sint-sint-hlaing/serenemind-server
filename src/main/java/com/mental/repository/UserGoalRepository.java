package com.mental.repository;

import com.mental.model.entity.User;
import com.mental.model.entity.UserGoal;
import com.mental.model.entity.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {

    // ===== Basic Queries =====
    List<UserGoal> findByUser(User user);

    List<UserGoal> findByUserAndStatus(User user, GoalStatus status);

    List<UserGoal> findByUserAndStatusIn(User user, List<GoalStatus> statuses);

    List<UserGoal> findByUserAndStatusOrderByCreatedAtDesc(User user, GoalStatus status);

    // ===== Count Queries =====
    long countByUser(User user);

    long countByUserAndStatus(User user, GoalStatus status);

    // ===== Expired Goals =====
    List<UserGoal> findByStatusAndTargetDateBefore(GoalStatus status, LocalDate date);

    // ===== Check if user has any active goals =====
    boolean existsByUserAndStatus(User user, GoalStatus status);

    long countByStatus(GoalStatus goalStatus);
}