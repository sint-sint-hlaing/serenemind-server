package com.mental.repository;

import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MoodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodTrackingRepository extends JpaRepository<MoodEntry, Long> {

    // ============================================================
    // USER BASED QUERIES - Using @Query
    // ============================================================

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email")
    List<MoodEntry> findByEmail(@Param("email") String email);

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email ORDER BY m.createdAt DESC")
    List<MoodEntry> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("SELECT m FROM MoodEntry m WHERE m.user = :user ORDER BY m.createdAt DESC")
    List<MoodEntry> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    // ============================================================
    // SINGLE RESULT QUERIES
    // ============================================================

    Optional<MoodEntry> findTopByUserAndDateOrderByCreatedAtDesc(User user, LocalDate date);

    Optional<MoodEntry> findTopByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.date = :date ORDER BY m.createdAt DESC")
    Optional<MoodEntry> findTopByUserEmailAndDateOrderByCreatedAtDesc(
            @Param("email") String email,
            @Param("date") LocalDate date
    );

    Optional<MoodEntry> findByUserAndDate(User user, LocalDate date);

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.date = :date")
    Optional<MoodEntry> findByUserEmailAndDate(
            @Param("email") String email,
            @Param("date") LocalDate date
    );

    // ============================================================
    // DATE RANGE QUERIES
    // ============================================================

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.date BETWEEN :startDate AND :endDate")
    List<MoodEntry> findByEmailAndDateBetween(
            @Param("email") String email,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.date BETWEEN :startDate AND :endDate ORDER BY m.date ASC")
    List<MoodEntry> findByUserEmailAndDateBetweenOrderByDateAsc(
            @Param("email") String email,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============================================================
    // USERNAME QUERIES
    // ============================================================

    @Query("SELECT m FROM MoodEntry m WHERE m.user.username = :username")
    List<MoodEntry> findByUserUsername(@Param("username") String username);

    // ============================================================
    // WEEKLY/MONTHLY QUERIES
    // ============================================================

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.createdAt >= :startDate")
    List<MoodEntry> findWeeklyData(
            @Param("email") String email,
            @Param("startDate") LocalDate startDate  // Changed from Instant to LocalDate
    );

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.createdAt >= :startDate")
    List<MoodEntry> findMonthlyData(
            @Param("email") String email,
            @Param("startDate") LocalDate startDate  // Changed from Instant to LocalDate
    );

    // ============================================================
    // MONTHLY HISTORY
    // ============================================================

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND YEAR(m.date) = :year AND MONTH(m.date) = :month ORDER BY m.date ASC")
    List<MoodEntry> findByYearAndMonth(
            @Param("email") String email,
            @Param("year") int year,
            @Param("month") int month
    );
    @Query("""
    SELECT m.mood, COUNT(m)
    FROM MoodEntry m
    WHERE m.date >= :startDate
    GROUP BY m.mood
""")
    List<Object[]> getMoodAnalysis(
            @Param("startDate") LocalDate startDate
    );

    // ============================================================
    // DISTRIBUTION QUERIES
    // ============================================================

    @Query("SELECT m.mood, COUNT(m) FROM MoodEntry m GROUP BY m.mood")
    List<Object[]> getMoodDistribution();
    @Query("SELECT COUNT(m) FROM MoodEntry m WHERE FUNCTION('DATE', m.createdAt) = CURRENT_DATE")
    long countToday();

    @Query("""
        SELECT m.mood
        FROM MoodEntry m
        GROUP BY m.mood
        ORDER BY COUNT(m) DESC
        LIMIT 1
    """)
    String findMostCommonMood();



    @Query("""
        SELECT AVG(m.score)
        FROM MoodEntry m
    """)
    Double findAverageScore();

    // ============================================================
    // COUNT QUERIES
    // ============================================================



    @Query("SELECT COUNT(m) FROM MoodEntry m WHERE m.mood = :mood")
    long countByMood(@Param("mood") MoodType mood);

    @Query("SELECT COUNT(m) FROM MoodEntry m WHERE m.mood = :mood AND m.date >= :startDate")
    long countByMoodAfterDate(
            @Param("mood") MoodType mood,
            @Param("startDate") LocalDate startDate
    );

    @Query("SELECT COUNT(m) FROM MoodEntry m WHERE m.user.email = :email")
    long countByUserEmail(@Param("email") String email);

    @Query("SELECT COUNT(m) FROM MoodEntry m WHERE m.user.email = :email AND m.mood = :mood")
    long countByUserEmailAndMood(
            @Param("email") String email,
            @Param("mood") MoodType mood
    );
}