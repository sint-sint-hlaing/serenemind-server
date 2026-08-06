package com.mental.model.entity.enums;

import lombok.Getter;

/**
 * Goal Status Enum
 */
@Getter
public enum GoalStatus {
    ACTIVE("Active", "Goal is in progress"),
    COMPLETED("Completed", "Goal has been completed successfully"),
    PAUSED("Paused", "Goal is temporarily paused"),
    CANCELLED("Cancelled", "Goal has been cancelled"),
    EXPIRED("Expired", "Goal has expired without completion"),
    ARCHIVED("Archived", "Goal has been archived");

    private final String displayName;
    private final String description;

    GoalStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Check if goal is still active (not completed or cancelled)
     */
    public boolean isActive() {
        return this == ACTIVE || this == PAUSED;
    }

    /**
     * Check if goal is finished (completed, cancelled, or expired)
     */
    public boolean isFinished() {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED;
    }
}