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

    private String emotion;

    private String sentiment;

    private int stressScore;

    private String stressLevel;

    private List<String> keyThemes;

    private String aiResponse;

    private String aiSuggestion;

    private Instant analysedAt;
}
