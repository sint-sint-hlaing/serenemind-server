package com.mental.repository;

import com.mental.model.entity.User;
import com.mental.model.entity.UserGoal;
import com.mental.model.entity.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {

    // ===== Basic Queries =====
    List<UserGoal> findByUser(User user);


    long countByUserIdAndStatus(Long userId, GoalStatus status);


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

    //this is new
    List<UserGoal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserGoal> findByUserIdAndStatus(Long userId, GoalStatus status);

    @Query("SELECT g FROM UserGoal g WHERE g.user.id = :userId AND g.status = 'ACTIVE'")
    List<UserGoal> findActiveGoalsByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(g.streak) FROM UserGoal g WHERE g.user.id = :userId AND g.status = 'ACTIVE'")
    Integer getTotalStreakByUser(@Param("userId") Long userId);


    @Query("SELECT g FROM UserGoal g WHERE g.user = :user AND g.status IN :statuses ORDER BY g.createdAt DESC")
    List<UserGoal> findByUserAndStatusInOrderByCreatedAtDesc(@Param("user") User user, @Param("statuses") List<GoalStatus> statuses);

    @Query("SELECT SUM(g.streak) FROM UserGoal g WHERE g.user = :user AND g.status = 'ACTIVE'")
    Integer getTotalStreakByUser(@Param("user") User user);

    @Query("SELECT g FROM UserGoal g WHERE g.user = :user AND g.status = 'ACTIVE' ORDER BY g.createdAt DESC LIMIT 5")
    List<UserGoal> findTop5ActiveByUser(@Param("user") User user);
}

