package com.mental.model.entity;

import com.mental.model.entity.enums.MeditationCategory;
import com.mental.model.entity.enums.MeditationStatus;
import com.mental.model.entity.enums.MeditationTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meditations")
@Getter
@Setter
public class Meditation extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeditationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeditationTime timeOfDay=MeditationTime.MORNING;

    @Column(nullable = false)
    private String duration; // seconds

    private String imageUrl;
    @Column(nullable = false)
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeditationStatus status = MeditationStatus.PUBLISHED;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(nullable = false)
    private Boolean premium = false;

    @Column(nullable = false)
    private Long listenCount = 0L;

    private boolean completed;



    @Column(nullable=false)
    private Integer durationSeconds;
    private Integer totalDurationMinutes;


    @Column(nullable=false)
    private Integer difficulty;

    @Column(name = "view_count")
    private Integer viewCount=0;
    @Column(name = "favorite_count")
    private Long favoriteCount = 0L;

    private LocalDateTime publishedAt;
    @OneToMany(mappedBy = "meditation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MeditationSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "meditation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MeditationFavorite> favorites = new ArrayList<>();
}