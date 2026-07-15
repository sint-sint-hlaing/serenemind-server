package com.mental.model.entity;

import com.mental.model.entity.enums.MoodType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mood_entries")
@Getter
@Setter
public class MoodEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private MoodType mood;

    private int intensity;

    private String note;
    private LocalDate date;

    @OneToOne(mappedBy = "moodEntry", cascade = CascadeType.ALL)
    private MoodAnalysis analysis;

}