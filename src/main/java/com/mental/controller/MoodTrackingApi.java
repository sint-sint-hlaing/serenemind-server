package com.mental.controller;

import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodEntryDto;
import com.mental.dto.mood.MoodRequest;
import com.mental.model.entity.MoodEntry;
import com.mental.security.UserPrincipal;
import com.mental.service.MoodTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/mood")
@RequiredArgsConstructor
public class MoodTrackingApi {
     private final MoodTrackingService moodTrackingService;

    @PostMapping("/save")
    public ResponseEntity<Void> saveMood(        @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody MoodRequest request) {
        moodTrackingService.saveMood(principal.getEmail(),request); // Service သို့ လွှဲပေးခြင်း[cite: 4]
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


     @GetMapping("/summary")
     public ResponseEntity<Map<String, Double>> getAllMoodHistory(@AuthenticationPrincipal UserPrincipal principal) {
         // 1. Get the map from the service
         Map<String, Double> summary = moodTrackingService.getMoodSummary(principal.getUsername());

         // 2. Wrap it in ResponseEntity.ok()
         return ResponseEntity.ok(summary);
     }
    @GetMapping("/history/{year}/{month}")
    public ResponseEntity<List<DailyMoodResponse>> getMoodHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(moodTrackingService.getMoodHistory(principal.getUsername(),year, month)); // လအလိုက် Data ရယူခြင်း[cite: 4]
    }
        @GetMapping("/weekly")
        public ResponseEntity<List<MoodEntry>> getWeeklyByStatus(@AuthenticationPrincipal UserPrincipal principal) {
            return ok(moodTrackingService.findWeeklyByStatus(principal.getEmail()));
    }

        @GetMapping("/monthly")
        public ResponseEntity<List<MoodEntry>> getMonthlyByStatus(@AuthenticationPrincipal UserPrincipal principal) {
            return ok(moodTrackingService.findMonthlyStatus(principal.getEmail()));

    }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteMood(@PathVariable Long id,@AuthenticationPrincipal UserPrincipal principal) {
            moodTrackingService.deleteMood(id, principal.getEmail());
            return ResponseEntity.noContent().build(); // 204 No Content
        }
    }


