package com.mental.controller;

import com.mental.dto.UserDto;
import com.mental.dto.analysis.JournalTrendDto;
import com.mental.dto.analysis.MeditationTrendDto;
import com.mental.dto.mood.MoodDistributionDto;
import com.mental.service.MoodTrackingService;
import com.mental.service.UserProfileService;
import com.mental.service.admin.AdminJournalService;
import com.mental.service.admin.AdminMeditationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Transactional(readOnly = true) // Analytics တွေမို့ Read-Only သတ်မှတ်ထားသည်
public class AdminAnalyticsController {

    private final MoodTrackingService moodService;
    private final UserProfileService userService;
    private final AdminMeditationService meditationService;
    private final AdminJournalService journalService;

    /**
     * Get Mood Analysis for the last N days.
     */
    @GetMapping("/mood/{days}")
    public ResponseEntity<List<MoodDistributionDto>> getMoodAnalysis(
            @PathVariable int days
    ) {
        return ResponseEntity.ok(moodService.getMoodAnalysis(days));
    }

    /**
     * Get Overall Mood Distribution percentages.
     */
    @GetMapping("/mood/distribution")
    public ResponseEntity<List<MoodDistributionDto>> getMoodDistribution() {
        return ResponseEntity.ok(moodService.getMoodDistribution());
    }

    /**
     * Get User Registration Growth Trend.
     */
    @GetMapping("/users/registration")
    public ResponseEntity<List<UserDto>> getUserRegistration() {
        return ResponseEntity.ok(userService.getUserRegistration());
    }

    /**
     * Get Meditation Usage Trend.
     */
    @GetMapping("/meditation/trend")
    public ResponseEntity<List<MeditationTrendDto>> getMeditationTrend() {
        return ResponseEntity.ok(meditationService.getMeditationTrend());
    }

    /**
     * Get Journal Activity Trend.
     */
    @GetMapping("/journal/trend")
    public ResponseEntity<List<JournalTrendDto>> getJournalTrend() {
        return ResponseEntity.ok(journalService.getJournalTrend());
    }
}