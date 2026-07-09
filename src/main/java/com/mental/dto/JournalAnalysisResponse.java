package com.mental.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * DTO for the Journal Analysis screen (Screen 7).
 * Contains AI-generated emotion, stress, themes, and suggestion data.
 */
@Getter
@Setter
public class JournalAnalysisResponse {

    private Long id;

    /** Primary emotion detected: e.g. "Calm", "Happy", "Anxious" */
    private String emotion;

    /** Overall sentiment: POSITIVE / NEUTRAL / NEGATIVE */
    private String sentiment;

    /**
     * Numeric stress score 0–100.
     * Maps to the progress bar on the Journal Analysis screen.
     */
    private int stressScore;

    /**
     * Human-readable stress level label: "Low", "Medium", "High"
     * Displayed alongside the stress progress bar.
     */
    private String stressLevel;

    /**
     * Key theme tags e.g. ["Gratitude", "Family", "Positivity"].
     * Displayed as chips on the Journal Analysis screen.
     */
    private List<String> keyThemes;

    /** Full AI reflection / response paragraph */
    private String aiResponse;

    /** Short actionable suggestion shown at the bottom of analysis */
    private String aiSuggestion;

    private Instant analysedAt;
}
