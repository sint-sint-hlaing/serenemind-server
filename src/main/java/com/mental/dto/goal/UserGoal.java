package com.mental.dto.goal;

import com.mental.model.entity.enums.GoalStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UserGoal(
        Long id,
        String username,
        String email,
        String title,
        String description,
        int targetDays,
        LocalDate targetDate,
        int progress,
        GoalStatus status,
        LocalDate createdAt,
        LocalDate updatedAt,
        LocalDate completedAt
) {
    // ===== Validation =====
    public UserGoal {
        if (targetDays < 1) {
            throw new IllegalArgumentException("Target days must be at least 1");
        }
        if (progress < 0) {
            throw new IllegalArgumentException("Progress cannot be negative");
        }
        if (progress > targetDays) {
            throw new IllegalArgumentException("Progress cannot exceed target days");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
    }

    // ===== Helper Methods =====
    public boolean isCompleted() {
        return status == GoalStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == GoalStatus.ACTIVE;
    }

    public double getProgressPercentage() {
        if (targetDays == 0) {
            return 0.0;
        }
        return (progress * 100.0) / targetDays;
    }

    public int getRemainingDays() {
        return Math.max(0, targetDays - progress);
    }

    public String getProgressText() {
        return progress + " / " + targetDays + " days";
    }

    public boolean isBehindSchedule() {
        if (targetDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(targetDate)) {
            return progress < targetDays;
        }
        return false;
    }

    public long getDaysUntilTarget() {
        if (targetDate == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(today, targetDate);
    }
}