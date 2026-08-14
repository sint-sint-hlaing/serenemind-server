package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeditationStatistics {
    private Long totalMeditations;
    private Long totalSessions;
    private Long totalMinutes;
    private Long currentStreak;
    private Long longestStreak;
}
