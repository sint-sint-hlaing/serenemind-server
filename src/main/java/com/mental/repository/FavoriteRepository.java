package com.mental.repository;

import com.mental.model.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndMeditationId(
            Long userId,
            Long meditationId);

    List<Favorite> findByUserId(Long userId);

    boolean existsByUserIdAndMeditationId(Long userId, Long meditationId);
}