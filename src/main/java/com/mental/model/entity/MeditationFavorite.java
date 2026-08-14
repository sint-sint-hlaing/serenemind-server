// MeditationFavorite.java
package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meditation_favorites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeditationFavorite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meditation_id", nullable = false)
    private Meditation meditation;
}