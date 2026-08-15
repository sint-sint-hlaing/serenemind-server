package com.mental.repository;

import com.mental.model.entity.GoalProgress;
import com.mental.model.entity.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalProgressRepository extends JpaRepository<GoalProgress, Long> {
    List<GoalProgress> findByGoalIdOrderByDateDesc(Long goalId);

    List<GoalProgress> findByGoalOrderByDateDesc(UserGoal goal);

    @Query("SELECT COUNT(p) FROM GoalProgress p WHERE p.goal.id = :goalId AND p.completed = true")
    Integer countCompletedByGoalId(@Param("goalId") Long goalId);

    @Query("SELECT p FROM GoalProgress p WHERE p.goal.id = :goalId AND p.date >= :startDate ORDER BY p.date DESC")
    List<GoalProgress> findRecentProgress(@Param("goalId") Long goalId, @Param("startDate") LocalDate startDate);

    @Query("SELECT p.date FROM GoalProgress p WHERE p.goal.id = :goalId AND p.completed = true ORDER BY p.date DESC")
    List<LocalDate> findCompletedDatesByGoalId(@Param("goalId") Long goalId);

    @Query("SELECT p FROM GoalProgress p WHERE p.goal.id = :goalId AND p.date = :date")
    Optional<GoalProgress> findByGoalIdAndDate(@Param("goalId") Long goalId, @Param("date") LocalDate date);

    void deleteByGoalId(Long goalId);

    List<GoalProgress> findByGoalIdAndDateBetween(Long goalId, LocalDate startDate, LocalDate endDate);
}
