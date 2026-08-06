package com.mental.model.entity;

import com.mental.model.entity.enums.MeditationCategory;
import com.mental.model.entity.enums.MeditationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "meditations")
@Getter
@Setter
public class Meditation extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String title;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeditationCategory categories;

    @Column(nullable = false)
    private String duration; // seconds

    private String imageUrl;
    @Column(nullable = false)
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeditationStatus status = MeditationStatus.DRAFT;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(nullable = false)
    private Boolean premium = false;

    @Column(nullable = false)
    private Long listenCount = 0L;
    private boolean completed;



    @Column(nullable=false)
    private Integer durationSeconds;


    @Column(nullable=false)
    private Integer difficulty;


    private Integer viewCount;


    private LocalDateTime publishedAt;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;}