package com.mental.repository;

import com.mental.model.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;


public interface FavoriteRepository
        extends JpaRepository<Favorite,Long> {


    boolean existsByUserIdAndMeditationId(
            Long userId,
            Long meditationId
    );

}