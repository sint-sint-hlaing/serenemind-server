package com.mental.repository;

import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodTrackingRepository
        extends JpaRepository<MoodEntry,Long> {


    List<MoodEntry> findByUserEmailOrderByCreatedAtDesc(
            String email
    );
    // MoodTrackingRepository.java ထဲတွင်
    Optional<MoodEntry> findTopByUserAndDateOrderByCreatedAtDesc(User user, LocalDate date);

    @Query("""
            SELECT m FROM MoodEntry m 
            WHERE m.user.email = :email 
            AND m.createdAt >= :startDate
            """)
    List<MoodEntry> findWeeklyData(
            @Param("email") String email,
            @Param("startDate") Instant startDate
    );
    @Query("""
        SELECT m FROM MoodEntry m
        WHERE m.user.email = :email
        AND m.createdAt >= :startDate
    """)
    List<MoodEntry> findMonthlyData(
            @Param("email") String email,
            @Param("startDate") Instant startDate
    );


    @Query("""
            SELECT m.mood, COUNT(m)
            FROM MoodEntry m
            GROUP BY m.mood
            """)
    List<Object[]> getMoodDistribution();


    @Query("""
SELECT m FROM MoodEntry m
WHERE m.user.email = :email
AND YEAR(m.date)=:year
AND MONTH(m.date)=:month
""")
    List<MoodEntry> findByYearAndMonth(
            @Param("email") String email,
            @Param("year") int year,
            @Param("month") int month
    );


    List<MoodEntry> findByUserUsername(
            String username
    );


    @Query("""
            SELECT m FROM MoodEntry m
            WHERE m.user.email = :email
            """)
    List<MoodEntry> findByEmail(
            @Param("email") String email
    );


    Optional<MoodEntry> findTopByUserOrderByCreatedAtDesc(
            User user
    );
    Optional<MoodEntry> findTopByUserEmailAndDateOrderByCreatedAtDesc(
            String email,
            LocalDate date
    );

    Optional<MoodEntry> findByUserAndDate(
            User user,
            LocalDate date
    );


    Optional<MoodEntry> findByUserEmailAndDate(
            String email,
            LocalDate date
    );

}

