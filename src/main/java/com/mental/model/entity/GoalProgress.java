package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "goal_progress")
public class GoalProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private UserGoal goal;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "value")
    private Double value; // For tracking numeric progress
}