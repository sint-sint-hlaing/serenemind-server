package com.mental.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JoinColumn(
            name="user_id",
            nullable=false
    )
    @JsonIgnore

    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private MoodType mood;

    @Column(nullable=false)
    private int intensity;

    @Column(nullable=false)
    private Integer score = 0;

    @Column(columnDefinition="TEXT")
    private String note;

    @Column(nullable=false)
    private LocalDate date;

    @OneToOne(
            mappedBy="moodEntry",
            cascade=CascadeType.ALL,
            fetch=FetchType.LAZY
    )
    private MoodAnalysis analysis;

}