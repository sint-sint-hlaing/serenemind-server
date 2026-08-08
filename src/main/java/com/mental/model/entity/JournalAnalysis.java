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

    private String emotion;

    private String sentiment;

    private int stressScore;

    private String stressLevel;

    @Column(length = 512)
    private String keyThemes;

    @Lob
    private String aiResponse;

    @Column(length = 1024)
    private String aiSuggestion;
}
