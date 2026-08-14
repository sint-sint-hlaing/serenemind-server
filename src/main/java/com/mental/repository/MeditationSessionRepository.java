package com.mental.repository;

import com.mental.dto.analysis.MeditationTrendDto;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
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
    @Query(value = """
        SELECT COUNT(*) 
        FROM meditation_sessions 
        WHERE DATE(created_at) = CURRENT_DATE
    """, nativeQuery = true)
    long countToday();

    @Query(value = """
        SELECT 
            DATE(created_at),
            COUNT(*)
        FROM meditation_sessions
        GROUP BY DATE(created_at)
        ORDER BY DATE(created_at)
    """, nativeQuery = true)
    List<Object[]> findMeditationTrend();

    List<MeditationSession> findByUserAndCompletedTrueOrderByCompletedAtDesc(User user);

    @Query("SELECT ms FROM MeditationSession ms WHERE ms.user = :user ORDER BY ms.completedAt DESC LIMIT 10")
    List<MeditationSession> findRecentSessions(@Param("user") User user);

    Optional<MeditationSession> findByUserAndMeditationId(User user, Long meditationId);

    @Query("SELECT COUNT(ms) FROM MeditationSession ms WHERE ms.meditation.id = :meditationId AND ms.completed = true")
    Long countCompletedSessions(@Param("meditationId") Long meditationId);

    @Query("SELECT ms.meditation FROM MeditationSession ms WHERE ms.user = :user AND ms.completed = true ORDER BY ms.completedAt DESC")
    List<Meditation> findCompletedMeditationsByUser(@Param("user") User user);


    @Query("SELECT COALESCE(SUM(ms.durationMinutes), 0) FROM MeditationSession ms")
    Long sumDurationMinutes();

    List<MeditationSession> findByUserAndCompletedTrue(User user);

    @Query("SELECT ms FROM MeditationSession ms WHERE ms.user = :user AND ms.completed = false")
    List<MeditationSession> findIncompleteSessionsByUser(User user);
}
