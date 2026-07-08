package com.mental.controller;

import com.mental.dto.mood.MoodEntryDto;
import com.mental.dto.mood.MoodRequest;
import com.mental.model.entity.MoodEntry;
import com.mental.security.UserPrincipal;
import com.mental.service.MoodTrackingService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<String> saveMood(@RequestBody MoodRequest request, Principal principal) {
        moodTrackingService.saveMood(principal.getName(), request);
        return ok("Mood saved successfully");
    }

     /**
        @PostMapping("/save")
        public ResponseEntity<MoodEntryDto> addMoodEntry(@RequestBody MoodEntryDto mood, @AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(moodTrackingService.saveMood(mood,principal));
        }**/

     @GetMapping("/summary")
     public ResponseEntity<Map<String, Double>> getAllMoodHistory(@AuthenticationPrincipal UserPrincipal principal) {
         // 1. Get the map from the service
         Map<String, Double> summary = moodTrackingService.getMoodSummary(principal.getUsername());

         // 2. Wrap it in ResponseEntity.ok()
         return ResponseEntity.ok(summary);
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


