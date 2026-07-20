package com.mental.dto.goal;

import lombok.Builder;

@Builder
public record GoalStatistics(
        long total,
        long active,
        long paused,
        long completed,
        long expired,
        long cancelled,
        double completionRate,
        long totalProgress
) {
    public GoalStatistics {
        if (completionRate < 0 || completionRate > 100) {
            throw new IllegalArgumentException("Completion rate must be between 0 and 100");
        }
        if (total < 0 || active < 0 || paused < 0 || completed < 0 || expired < 0 || cancelled < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
    }

    public String getFormattedCompletionRate() {
        return String.format("%.1f%%", completionRate);
    }

    public long getTotalActive() {
        return active + paused;
    }

    public boolean hasGoals() {
        return total > 0;
    }

    public boolean isOnTrack() {
        return completionRate >= 50.0;
    }
}