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
public interface MoodTrackingRepository extends JpaRepository<MoodEntry,Long> {
    List<MoodEntry> findByUserEmailOrderByCreatedAtDesc(String email);
    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.createdAt >= :startDate")
    List<MoodEntry> findWeeklyData(@Param("email") String email, @Param("startDate") Instant startDate);

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.createdAt >= :startDate")
    List<MoodEntry> findMonthlyData(@Param("email") String email, @Param("startDate") Instant startDate);

    @Query("SELECT m.mood, COUNT(m) FROM MoodEntry m GROUP BY m.mood")
    List<Object[]> getMoodDistribution();
    @Query("SELECT m FROM MoodEntry m WHERE m.user.username = :username " +
            "AND YEAR(m.createdAt) = :year AND MONTH(m.createdAt) = :month")
    List<MoodEntry> findByYearAndMonth(String username, int year, int month);

    List<MoodEntry> findByUserUsername(String username);

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email")
    List<MoodEntry> findByEmail(@Param("email") String email);

    Optional<MoodEntry> findTopByUserOrderByCreatedAtDesc(User user);
    Optional<MoodEntry> findByUserAndDate(
            User user,
            LocalDate date
    );
}
