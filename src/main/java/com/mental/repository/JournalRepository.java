package com.mental.repository;

import com.mental.dto.admin.JournalAdminDto;
import com.mental.dto.analysis.JournalTrendDto;
import com.mental.model.entity.Journal;
import com.mental.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    /** Get all journals for a user ordered by newest first */
    @EntityGraph(attributePaths = {"analysis"})
    List<Journal> findByUserOrderByCreatedAtDesc(User user);

    /** Get only favourite journals for a user */
    @EntityGraph(attributePaths = {"analysis"})
    List<Journal> findByUserAndFavouriteTrueOrderByCreatedAtDesc(User user);

    /** Get all tagged (has tags) journals for a user */
    @Query("SELECT j FROM Journal j LEFT JOIN FETCH j.analysis WHERE j.user = :user AND j.tags IS NOT NULL AND j.tags <> '' ORDER BY j.createdAt DESC")
    List<Journal> findTaggedByUser(@Param("user") User user);

    /** Case-insensitive tag matching (when query starts with '#') */
    @Query("SELECT j FROM Journal j LEFT JOIN FETCH j.analysis WHERE j.user = :user AND LOWER(j.tags) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY j.createdAt DESC")
    List<Journal> searchByUserAndTagOnly(@Param("user") User user, @Param("query") String query);

    /** Case-insensitive search across both title and tags */
    @Query("SELECT j FROM Journal j LEFT JOIN FETCH j.analysis WHERE j.user = :user AND (LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY j.createdAt DESC")
    List<Journal> searchByUserAndTitleOrTag(@Param("user") User user, @Param("query") String query);

    Page<Journal> findAllByFlaggedTrueOrderByCreatedAtDesc(Pageable pageable);

    List<Journal> findByUser(User user);

    long countByUser(User user);

   

    @Query(value = """
        SELECT 
            DATE(created_at),
            COUNT(*)
        FROM journals
        GROUP BY DATE(created_at)
        ORDER BY DATE(created_at)
    """, nativeQuery = true)
    List<Object[]> findJournalTrend();

    @Query("""
        SELECT j
        FROM Journal j
        JOIN FETCH j.user
        ORDER BY j.createdAt DESC
    """)
    List<JournalAdminDto> findAllJournalForAdmin();

    @Query("""
SELECT COUNT(j)
FROM Journal j
WHERE DATE(j.createdAt)=CURRENT_DATE
""")
    long countToday();



    long countByFlaggedTrue();
    Page<Journal> findAllByFlaggedTrue(
            Pageable pageable
    );

    long countByCreatedAtBetween(LocalDateTime localDateTime, LocalDateTime now);
}