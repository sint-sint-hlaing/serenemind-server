package com.mental.model.entity;

import com.mental.model.entity.enums.MoodType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @OneToOne(mappedBy = "moodEntry", cascade = CascadeType.ALL)
    private MoodAnalysis analysis;

}