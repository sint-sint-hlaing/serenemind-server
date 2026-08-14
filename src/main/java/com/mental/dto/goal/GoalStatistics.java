package com.mental.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalStatistics {
    private long total;
    private long active;
    private long paused;
    private long completed;
    private long expired;
    private long cancelled;
    private long totalProgress;
    private double completionRate;
    private int totalStreak;  // Added
    private int currentStreak; // Added
}