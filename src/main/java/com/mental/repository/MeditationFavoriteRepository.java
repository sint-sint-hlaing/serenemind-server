package com.mental.repository;

import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationFavorite;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeditationFavoriteRepository extends JpaRepository<MeditationFavorite, Long> {

    Optional<MeditationFavorite> findByUserAndMeditation(User user, Meditation meditation);

    List<MeditationFavorite> findByUser(User user);

    void deleteByUserAndMeditation(User user, Meditation meditation);

    boolean existsByUserAndMeditation(User user, Meditation meditation);}
