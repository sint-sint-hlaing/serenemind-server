package com.mental.dto.Streak;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class StreakResponse {
    private int currentStreak;
    private int longestStreak;
    private int streakFreezeCount;
    private boolean isNewBest; // true ဆိုလျှင် Screen 11 (Celebration Pop-up) ပြရန်
    private List<Boolean> weeklyOverview; // Screen 3 ရဲ့ အမှန်ခြစ် ၇ ခုအတွက် (Mon-Sun)
}