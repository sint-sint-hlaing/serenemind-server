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

    // Fetch all reminders belonging to a specific user
    List<Reminder> findByUserId(Long userId);

    // Find a specific reminder belonging to a specific user (for security/ownership validation)
    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

//    @Query("SELECT r FROM Reminder r WHERE r.enabled = true AND " +
//            "CAST(CONCAT(r.startDate, 'T', r.reminderTime) AS localdatetime) <= :now")
//    List<Reminder> findPendingReminders(@Param("now") LocalDateTime now);

    @Query("SELECT r FROM Reminder r WHERE r.enabled = true AND (" +
            "r.startDate < :currentDate OR " +
            "(r.startDate = :currentDate AND r.reminderTime <= :currentTime))")
    List<Reminder> findPendingReminders(
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    @Query("SELECT r FROM Reminder r WHERE r.userId = :userId " +
            "ORDER BY CASE WHEN r.enabled = true THEN 0 ELSE 1 END ASC, " +
            "r.startDate ASC, r.reminderTime ASC")
    List<Reminder> findByUserIdSorted(@Param("userId") Long userId);

    List<Reminder> findByEnabledTrueAndReminderTimeAndStartDateLessThanEqual(LocalTime reminderTime, LocalDate currentDate);
}
