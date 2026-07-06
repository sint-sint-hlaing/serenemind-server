package com.mental.repository;

import com.mental.model.entity.MoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MoodTrackingRepository extends JpaRepository<MoodEntry,Long> {
    List<MoodEntry> findByUserEmailOrderByCreatedAtDesc(String email);
    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.createdAt >= :startDate")
    List<MoodEntry> findWeeklyData(@Param("email") String email, @Param("startDate") Instant startDate);

    @Query("SELECT m FROM MoodEntry m WHERE m.user.email = :email AND m.createdAt >= :startDate")
    List<MoodEntry> findMonthlyData(@Param("email") String email, @Param("startDate") Instant startDate);

    @Query("SELECT m.mood, COUNT(m) FROM MoodEntry m GROUP BY m.mood")
    List<Object[]> getMoodDistribution();

}
