package com.mental.service;

import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodEntryDto;
import com.mental.dto.mood.MoodRequest;
import com.mental.model.entity.MoodEntry;
import com.mental.security.UserPrincipal;

import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface MoodTrackingService{
    void saveMood(String email, MoodRequest request);

    Map<String, Double> getMoodSummary(String username);

    List<DailyMoodResponse> getMoodHistory(String username, int year, int month);

    List<MoodEntry> findWeeklyByStatus(String email);

    List<MoodEntry> findMonthlyStatus(String email);

    void deleteMood(Long id, String email);


    /**MoodEntryDto saveMood(MoodEntryDto mood, UserPrincipal principal);*/

   // List<MoodEntryDto> findAllMoods(String name);
}
