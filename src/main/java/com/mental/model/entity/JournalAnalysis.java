package com.mental.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "journal_analysis")
@Getter
@Setter
public class JournalAnalysis extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "journal_id")
    private Journal journal;

    /** Primary detected emotion e.g. "Calm", "Happy", "Anxious" */
    private String emotion;

    /** Overall sentiment: POSITIVE / NEUTRAL / NEGATIVE */
    private String sentiment;

    /** Stress score 0-100 */
    private int stressScore;

    /** Human-readable stress level: Low / Medium / High */
    private String stressLevel;

    /** Comma-separated key theme tags e.g. "Gratitude,Family,Positivity" */
    @Column(length = 512)
    private String keyThemes;

    /** Full AI-generated response / reflection text */
    @Lob
    private String aiResponse;

    /** Short actionable AI suggestion shown below analysis */
    @Column(length = 1024)
    private String aiSuggestion;
}