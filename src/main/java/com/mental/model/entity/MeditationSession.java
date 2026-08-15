package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.ConnectionBuilder;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "meditation_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeditationSession extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "meditation_id",
            nullable = false
    )
    private Meditation meditation;


    @Column(nullable = false)
    private boolean completed = false;

    private Integer durationMinutes;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    // When timer should finish
    @Column(name = "ends_at")
    private LocalDateTime endsAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;



}
