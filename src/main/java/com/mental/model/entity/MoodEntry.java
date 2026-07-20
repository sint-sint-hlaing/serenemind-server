package com.mental.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mental.model.entity.enums.MoodType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mood_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoodType mood;

    @Column(nullable = false)
    private int intensity;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDate date;

    @OneToOne(mappedBy = "moodEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MoodAnalysis analysis;

    // ===== Helper Methods =====

    public String getMoodName() {
        return mood != null ? mood.name() : "UNKNOWN";
    }

    public String getMoodEmoji() {
        return mood != null ? mood.getEmoji() : "😐";
    }

    public String getMoodMessage() {
        return mood != null ? mood.getMessage() : "No mood recorded";
    }

    public int getMoodPercentage() {
        return mood != null ? mood.getPercentage() : 0;
    }

    public boolean isHappy() {
        return mood == MoodType.HAPPY;
    }

    public boolean isCalm() {
        return mood == MoodType.CALM;
    }

    public boolean isNeutral() {
        return mood == MoodType.NEUTRAL;
    }

    public boolean isSad() {
        return mood == MoodType.SAD;
    }

    public boolean isAnxious() {
        return mood == MoodType.ANXIOUS;
    }

    public boolean isAngry() {
        return mood == MoodType.ANGRY;
    }

    public boolean isPositive() {
        return mood == MoodType.HAPPY || mood == MoodType.CALM;
    }

    public boolean isNegative() {
        return mood == MoodType.SAD || mood == MoodType.ANXIOUS || mood == MoodType.ANGRY;
    }
}