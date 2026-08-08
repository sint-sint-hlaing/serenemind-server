package com.mental.repository;

import com.mental.model.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {


    List<Reminder> findByUserId(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM Reminder r WHERE r.enabled = true AND " +
            "r.startDate = :currentDate AND r.reminderTime = :currentTime")
    List<Reminder> findPendingReminders(
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    @Query("SELECT r FROM Reminder r WHERE r.userId = :userId " +
            "ORDER BY " +
            "  CASE WHEN r.enabled = true THEN 0 ELSE 1 END ASC, " +
            "  CASE " +
            "    WHEN r.startDate = :currentDate AND r.reminderTime < :currentTime THEN 1 " +
            "    ELSE 0 " +
            "  END ASC, " +
            "  r.startDate ASC, " +
            "  r.reminderTime ASC")
    List<Reminder> findByUserIdSorted(
            @Param("userId") Long userId,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    List<Reminder> findByEnabledTrueAndReminderTimeAndStartDateLessThanEqual(LocalTime reminderTime, LocalDate currentDate);
}
