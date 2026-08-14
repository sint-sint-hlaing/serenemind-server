package com.mental.dto.Steak;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class StreakResponse {
    private int currentStreak;
    private int longestStreak;
    private int streakFreezeCount;
    private boolean isNewBest;
    private List<Boolean> weeklyOverview;
}