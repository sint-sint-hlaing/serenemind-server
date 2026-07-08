package com.mental.service;

import com.mental.dto.mood.MoodEntryDto;
import com.mental.dto.mood.MoodRequest;
import com.mental.model.entity.MoodEntry;
import com.mental.security.UserPrincipal;

import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface MoodTrackingService{


    /**MoodEntryDto saveMood(MoodEntryDto mood, UserPrincipal principal);*/

   // List<MoodEntryDto> findAllMoods(String name);

    List<MoodEntry> findWeeklyByStatus(String name);

    List<MoodEntry> findMonthlyStatus(String name);

    void deleteMood(Long id, String name);

    void saveMood(String username, MoodRequest request);

    Map<String, Double> getMoodSummary(String username);
}
