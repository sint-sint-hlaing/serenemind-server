package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_streaks")
@Getter
@Setter
public class UserStreak extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private int streakCount;
    private LocalDate lastCompleted;
    @Column(name = "longest_streak")
    private int longestStreak = 0;
}