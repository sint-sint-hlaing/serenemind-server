package com.mental.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
}