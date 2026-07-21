package com.mental.repository;

import com.mental.model.entity.Meditation;
import com.mental.model.entity.enums.MeditationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;


public interface MeditationRepository
        extends JpaRepository<Meditation, Long> {



    List<Meditation> findByCategories(
            MeditationCategory categories
    );



    @Query("""
        SELECT m
        FROM Meditation m
        WHERE LOWER(m.title)
        LIKE LOWER(CONCAT('%', :keyword, '%'))
        AND m.status = 'PUBLISHED'
    """)
    List<Meditation> search(
            @Param("keyword") String keyword
    );



    @Query("""
        SELECT m
        FROM Meditation m
        WHERE m.categories IN
        (
            SELECT s.meditation.categories
            FROM MeditationSession s
            WHERE s.user.id = :userId
        )
        AND m.status = 'PUBLISHED'
        ORDER BY m.createdAt DESC
    """)
    List<Meditation> findRecommendedMeditations(
            @Param("userId") Long userId
    );

    @Query("""
    SELECT m
    FROM Meditation m
    WHERE LOWER(m.title)
    LIKE LOWER(CONCAT('%', :keyword, '%'))
    AND m.status = 'PUBLISHED'
""")
    List<Meditation> searchByKeyword(
            @Param("keyword") String keyword
    );

    long countByCompletedTrue();


    @Query("""
SELECT COUNT(DISTINCT m.user.id)
FROM MeditationSession m
""")
    long countDistinctUsers();
}