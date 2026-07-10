package com.mental.dto.Breathing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BreathingSummaryResponse {
    private String duration;      // e.g., "3:00"
    private int rounds;          // e.g., 4
    private int totalBreaths;    // e.g., 12
}