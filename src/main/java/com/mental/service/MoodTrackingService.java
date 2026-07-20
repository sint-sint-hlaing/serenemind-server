package com.mental.service;

import com.mental.dto.mood.WeeklyMoodResponse;
import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodRequest;
import com.mental.model.entity.MoodEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MoodTrackingService {


    void saveMood(
            String email,
            MoodRequest request
    );


    Map<String,Double> getMoodSummary(
            String email
    );


    List<DailyMoodResponse> getMoodHistory(
            String email,
            int year,
            int month
    );


    DailyMoodResponse getMoodByDate(
            String email,
            LocalDate date
    );


    List<MoodEntry> findWeeklyByStatus(
            String email
    );


    List<MoodEntry> findMonthlyStatus(
            String email
    );


    WeeklyMoodResponse getWeeklyMood(
            String email
    );


    void deleteMood(
            Long id,
            String email
    );

}