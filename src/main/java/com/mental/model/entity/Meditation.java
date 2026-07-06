package com.mental.model.entity;

import com.mental.model.entity.enums.MeditationCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

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
    private MeditationCategory category;


    @Column(nullable = false)
    private Integer duration; // seconds


    private String audioUrl;


    private String description;
}