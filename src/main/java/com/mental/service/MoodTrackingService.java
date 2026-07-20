package com.mental.service;

import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodDistributionDto;
import com.mental.dto.mood.MoodRequest;
import com.mental.dto.mood.WeeklyMoodResponse;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.enums.MoodType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MoodTrackingService {

    // ===== Save =====
    void saveMood(String email, MoodRequest request);

    // ===== Summary =====
    Map<String, Double> getMoodSummary(String email);

    // ===== History =====
    List<DailyMoodResponse> getMoodHistory(String email, int year, int month);

    // ===== By Date =====
    DailyMoodResponse getMoodByDate(String email, LocalDate date);

    // ===== Weekly =====
    List<WeeklyMoodResponse> getWeeklyMood(String email);

    // ===== Monthly =====
    List<DailyMoodResponse> getMonthlyMood(String email);

    // ===== Weekly Summary =====
    WeeklyMoodResponse getWeeklySummary(String email);

    // ===== Delete =====
    void deleteMood(Long id, String email);

    // ===== Distribution =====
    List<MoodDistributionDto> getMoodDistribution();

    // ===== Analysis =====
    List<MoodDistributionDto> getMoodAnalysis(int days);

    // ===== Counts =====


    // ===== Deprecated =====
    @Deprecated
    List<MoodEntry> findWeeklyByStatus(String email);

    @Deprecated
    List<MoodEntry> findMonthlyStatus(String email);
}