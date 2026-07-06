package com.mental.model.entity;

import com.mental.model.entity.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;

@Entity
@Table(name = "user_goals")
@Getter
@Setter
public class UserGoal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String title;
    private String description;


    private int targetDays;
    private int progress;

    @Enumerated(EnumType.STRING)
    private GoalStatus status;
}
