package com.mental.dto.Breathing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BreathingSessionResponse {
    private String sessionId;
    private String exerciseType;
    private int totalDurationSeconds;
    private int estimatedRounds;
}