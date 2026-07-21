package com.mental.repository;

import com.mental.dto.admin.JournalAdminDto;
import com.mental.dto.analysis.JournalTrendDto;
import com.mental.model.entity.Journal;
import com.mental.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    List<Journal> findByUserOrderByCreatedAtDesc(User user);

    List<Journal> findByUserAndFavouriteTrueOrderByCreatedAtDesc(User user);

    @Query("SELECT j FROM Journal j WHERE j.user = :user AND j.tags IS NOT NULL AND j.tags <> '' ORDER BY j.createdAt DESC")
    List<Journal> findTaggedByUser(@Param("user") User user);

    @Query("SELECT j FROM Journal j WHERE j.user = :user AND LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY j.createdAt DESC")
    List<Journal> searchByUserAndTitle(@Param("user") User user, @Param("query") String query);

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