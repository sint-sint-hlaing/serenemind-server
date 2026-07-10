package com.mental.model.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "breathing_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreathingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String exerciseType;

    @Column(nullable = false)
    private int targetDurationMinutes;

    private int completedRounds;

    private int calculatedBreaths;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
