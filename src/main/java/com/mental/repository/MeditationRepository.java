package com.mental.repository;

import com.mental.model.entity.Meditation;
import com.mental.model.entity.enums.MeditationCategory;
import com.mental.model.entity.enums.MeditationStatus;
import com.mental.model.entity.enums.MeditationTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MeditationRepository extends JpaRepository<Meditation, Long> {

    // ===== NAVIGATION =====

    Optional<Meditation> findFirstByIdLessThanOrderByIdDesc(Long id);

    Optional<Meditation> findFirstByIdGreaterThanOrderByIdAsc(Long id);

    // ===== SEARCH =====

    @Query("""
        SELECT m
        FROM Meditation m
        WHERE m.status = com.mental.model.entity.enums.MeditationStatus.PUBLISHED
        AND (
            :query IS NULL
            OR LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (
            :category IS NULL
            OR m.category = :category
        )
        ORDER BY m.createdAt DESC
    """)
    List<Meditation> search(
            @Param("query") String query,
            @Param("category") MeditationCategory category
    );

    @Query("""
        SELECT m
        FROM Meditation m
        WHERE m.status = com.mental.model.entity.enums.MeditationStatus.PUBLISHED
        AND (
            :query IS NULL
            OR LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (
            :category IS NULL
            OR m.category = :category
        )
        AND (
            :time IS NULL
            OR m.timeOfDay = :time
        )
        ORDER BY m.createdAt DESC
    """)
    List<Meditation> search(
            @Param("query") String query,
            @Param("category") MeditationCategory category,
            @Param("time") MeditationTime time
    );
    @Query("SELECT m FROM Meditation m WHERE m.status = 'PUBLISHED' AND " +
            "(LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Meditation> searchMeditations(@Param("query") String query);

    @Query("SELECT m FROM Meditation m WHERE m.status = 'PUBLISHED' AND " +
            "(:category IS NULL OR m.category = :category) AND " +
            "(:time IS NULL OR m.timeOfDay = :time) AND " +
            "(:query IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Meditation> searchWithFilters(
            @Param("query") String query,
            @Param("category") MeditationCategory category,
            @Param("time") MeditationTime time
    );
    @Query("""
        SELECT m
        FROM Meditation m
        WHERE m.category IN (
            SELECT s.meditation.category
            FROM MeditationSession s
            WHERE s.user.id = :userId
        )
        AND m.status = com.mental.model.entity.enums.MeditationStatus.PUBLISHED
        ORDER BY m.createdAt DESC
    """)
    List<Meditation> findRecommendedMeditations(@Param("userId") Long userId);

        @Query("SELECT m FROM Meditation m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND m.status = 'PUBLISHED'")
    List<Meditation> searchByKeyword(@Param("keyword") String keyword);

    // ===== COUNT METHODS =====

    @Query("SELECT COUNT(m) FROM Meditation m WHERE m.status = 'PUBLISHED' AND m.featured = true")
    long countByFeaturedTrue();

    @Query("SELECT COUNT(DISTINCT m.user.id) FROM MeditationSession m")
    long countDistinctUsers();

    // ===== LIST METHODS =====

    List<Meditation> findByStatusAndFeaturedTrueOrderByListenCountDesc(MeditationStatus status);

    List<Meditation> findByStatusAndCategoryOrderByCreatedAtDesc(MeditationStatus status, MeditationCategory category);

    @Query("SELECT m FROM Meditation m WHERE m.status = 'PUBLISHED' ORDER BY m.listenCount DESC")
    List<Meditation> findPopularMeditations(Pageable pageable);

    @Query("SELECT m FROM Meditation m WHERE m.status = 'PUBLISHED' ORDER BY m.createdAt DESC")
    List<Meditation> findRecentMeditations(Pageable pageable);

    // ===== CATEGORY =====

    @Query("SELECT m FROM Meditation m WHERE m.status = 'PUBLISHED' AND m.category = :category")
    List<Meditation> findByCategory(@Param("category") MeditationCategory category);

    // ===== STATUS =====
    List<Meditation> findByTitleContainingIgnoreCase(String title);

    long countByStatus(MeditationStatus meditationStatus);

    List<Meditation> findByCategoryAndStatus(MeditationCategory meditationCategory, MeditationStatus meditationStatus);

    List<Meditation> findByTimeOfDay(MeditationTime timeOfDay);

    List<Meditation> findByStatusOrderByCreatedAtDesc(MeditationStatus status);


    // ဒါမှမဟုတ် List အနေနဲ့ ပြန်ချင်ရင်
       long countByCompletedTrue();
}