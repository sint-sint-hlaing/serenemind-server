package com.mental.controller;

import com.mental.dto.mood.MoodEntryDto;
import com.mental.model.entity.MoodEntry;
import com.mental.security.UserPrincipal;
import com.mental.service.MoodTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mood")
@RequiredArgsConstructor
public class MoodTrackingApi {
     private final MoodTrackingService moodTrackingService;

        @PostMapping
        public ResponseEntity<MoodEntryDto> addMoodEntry(@RequestBody MoodEntryDto mood, @AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(moodTrackingService.saveMood(mood,principal));
        }

        @GetMapping
        public ResponseEntity<List<MoodEntryDto>> getAllMoodHistory(@AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(moodTrackingService.findAllMoods(principal.getEmail()));
        }
        @GetMapping("/weekly")
        public ResponseEntity<List<MoodEntry>> getWeeklyByStatus(@AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(moodTrackingService.findWeeklyByStatus(principal.getEmail()));
    }

        @GetMapping("/monthly")
        public ResponseEntity<List<MoodEntry>> getMonthlyByStatus(@AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(moodTrackingService.findMonthlyStatus(principal.getEmail()));

    }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteMood(@PathVariable Long id,@AuthenticationPrincipal UserPrincipal principal) {
            moodTrackingService.deleteMood(id, principal.getEmail());
            return ResponseEntity.noContent().build(); // 204 No Content
        }
    }


