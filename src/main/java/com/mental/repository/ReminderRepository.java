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

    @Query("SELECT r FROM Reminder r WHERE r.enabled = true AND " +
            "r.startDate = :currentDate AND r.reminderTime = :currentTime")
    List<Reminder> findPendingReminders(
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    @Query("SELECT r FROM Reminder r WHERE r.userId = :userId " +
            "ORDER BY " +
            // ၁။ ဖွင့်ထားတဲ့ (Active) Reminder တွေကို အပေါ်ဆုံး တင်မယ်
            "  CASE WHEN r.enabled = true THEN 0 ELSE 1 END ASC, " +

            // ၂။ ဖုန်း Alarm လော့ဂျစ်အတိုင်း အချိန်ကျော်ရင် မနက်ဖြန် (1)၊ မကျော်ရင် ဒီနေ့ (0) ဟု သတ်မှတ်၍ စီမယ်
            "  CASE " +
            "    WHEN r.startDate = :currentDate AND r.reminderTime < :currentTime THEN 1 " +
            "    ELSE 0 " +
            "  END ASC, " +

            // ၃။ ရက်စွဲအလိုက် ထပ်စီမယ် (မနက်ဖြန်ကောင်တွေက ဒီနေ့ကောင်တွေရဲ့ အောက်ကို ရောက်သွားမယ်)
            "  r.startDate ASC, " +

            // ၄။ နောက်ဆုံးမှ အချိန် (Reminder Time) အလိုက် အစီအစဉ်တကျ ကွက်တိ စီမယ်
            "  r.reminderTime ASC")
    List<Reminder> findByUserIdSorted(
            @Param("userId") Long userId,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    List<Reminder> findByEnabledTrueAndReminderTimeAndStartDateLessThanEqual(LocalTime reminderTime, LocalDate currentDate);
}
