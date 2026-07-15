package com.mental.repository;

import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface MeditationSessionRepository
        extends JpaRepository<MeditationSession,Long> {


    List<MeditationSession> findByUserOrderByCreatedAtDesc(
            User user
    );

    @Query("""
        SELECT s.meditation
        FROM MeditationSession s
        WHERE s.user.id = :userId
        ORDER BY s.createdAt DESC
    """)
    List<Meditation> findContinueListening(
            @Param("userId") Long userId
    );
}
