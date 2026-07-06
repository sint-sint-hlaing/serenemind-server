package com.mental.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mood_analysis")
@Getter
@Setter
public class MoodAnalysis extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "mood_entry_id")
    private MoodEntry moodEntry;

    private String emotion;
    private int stressLevel;
    private String recommendation;
}