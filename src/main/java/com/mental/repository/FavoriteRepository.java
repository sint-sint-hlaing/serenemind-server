package com.mental.repository;

import com.mental.model.entity.Favorite;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndMeditationId(
            Long userId,
            Long meditationId);

    List<Favorite> findByUserId(Long userId);

    boolean existsByUserIdAndMeditationId(Long userId, Long meditationId);

    boolean existsByUserAndMeditation(User user, Meditation meditation);

    void deleteByUserAndMeditation(User user, Meditation meditation);
}