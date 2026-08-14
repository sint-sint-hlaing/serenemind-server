package com.mental.model.entity;

import com.mental.dto.goal.GoalNoteDTO;
import com.mental.model.entity.enums.Frequency;
import com.mental.model.entity.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_goals")
public class UserGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column(name = "target_days", nullable = false)
    private int targetDays;

    @Column(nullable = false)
    private int progress = 0;

    @Column(name = "unit")
    private String unit;

    @Column(name = "icon")
    private String icon = "📚";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "silent_mode")
    private Boolean silentMode = false;

    @Column(name = "streak")
    private int streak = 0;


    @Column(name = "target_date")
    private LocalDate targetDate;




    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GoalProgress> progressHistory = new ArrayList<>();

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GoalNote> notes = new ArrayList<>();

}