package com.mental.controller;

import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodRequest;
import com.mental.dto.mood.WeeklyMoodResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.MoodTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mood")
@RequiredArgsConstructor
@Tag(name = "Mood Tracking", description = "Mood tracking endpoints")
public class MoodTrackingApi {

    private final MoodTrackingService moodTrackingService;

    // ===== SAVE =====
    @Operation(summary = "Save mood entry")
    @PostMapping("/save")
    public ResponseEntity<Void> saveMood(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MoodRequest request) {
        log.info("Saving mood for user: {}", principal.getEmail());
        moodTrackingService.saveMood(principal.getEmail(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ===== SUMMARY =====
    @Operation(summary = "Get mood summary")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Double>> getMoodSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching mood summary for user: {}", principal.getEmail());
        return ResponseEntity.ok(moodTrackingService.getMoodSummary(principal.getEmail()));
    }

    // ===== HISTORY (Monthly) =====
    @Operation(summary = "Get monthly mood history")
    @GetMapping("/history")
    public ResponseEntity<List<DailyMoodResponse>> getMonthlyHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam int year,
            @RequestParam int month) {
        log.debug("Fetching mood history for user: {} - {}/{}", principal.getEmail(), year, month);
        return ResponseEntity.ok(moodTrackingService.getMoodHistory(principal.getEmail(), year, month));
    }

    // ===== BY DATE =====
    @Operation(summary = "Get mood by date")
    @GetMapping("/date/{date}")
    public ResponseEntity<DailyMoodResponse> getMoodByDate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable LocalDate date) {
        log.debug("Fetching mood for user: {} on date: {}", principal.getEmail(), date);
        return ResponseEntity.ok(moodTrackingService.getMoodByDate(principal.getEmail(), date));
    }

    // ===== WEEKLY (Individual Daily Entries) =====
    @Operation(summary = "Get weekly mood - individual daily entries")
    @GetMapping("/weekly")
    public ResponseEntity<List<WeeklyMoodResponse>> getWeeklyMood(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching weekly mood for user: {}", principal.getEmail());
        return ResponseEntity.ok(moodTrackingService.getWeeklyMood(principal.getEmail()));
    }

    // ===== MONTHLY =====
    @Operation(summary = "Get monthly mood")
    @GetMapping("/monthly")
    public ResponseEntity<List<DailyMoodResponse>> getMonthlyMood(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching monthly mood for user: {}", principal.getEmail());
        return ResponseEntity.ok(moodTrackingService.getMonthlyMood(principal.getEmail()));
    }

    // ===== WEEKLY SUMMARY =====
    @Operation(summary = "Get weekly summary with dominant mood")
    @GetMapping("/summary/week")
    public ResponseEntity<WeeklyMoodResponse> getWeeklySummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching weekly summary for user: {}", principal.getEmail());
        return ResponseEntity.ok(moodTrackingService.getWeeklySummary(principal.getEmail()));
    }

    // ===== DELETE =====
    @Operation(summary = "Delete mood entry")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteMood(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Deleting mood entry: {} for user: {}", id, principal.getEmail());
        moodTrackingService.deleteMood(id, principal.getEmail());
        return ResponseEntity.noContent().build();
    }
}