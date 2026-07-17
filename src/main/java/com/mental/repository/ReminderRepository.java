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

    @Query("SELECT r FROM Reminder r WHERE r.enabled = true AND " +
            "CAST(CONCAT(r.startDate, 'T', r.reminderTime) AS localdatetime) <= :now")
    List<Reminder> findPendingReminders(@Param("now") LocalDateTime now);

    List<Reminder> findByEnabledTrueAndReminderTimeAndStartDateLessThanEqual(LocalTime reminderTime, LocalDate currentDate);
}
