package com.mental.controller;


import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodRequest;
import com.mental.dto.mood.WeeklyMoodResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.MoodTrackingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/mood")
@RequiredArgsConstructor
public class MoodTrackingApi {


    private final MoodTrackingService moodTrackingService;



    // Save Mood
    // POST /api/moods/save

    @PostMapping("/save")
    public ResponseEntity<Void> saveMood(

            @AuthenticationPrincipal UserPrincipal principal,

            @RequestBody MoodRequest request

    ){

        moodTrackingService.saveMood(
                principal.getEmail(),
                request
        );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }



    // Overall Summary
    // GET /api/moods/summary

    @GetMapping("/summary")
    public ResponseEntity<Map<String,Double>> getMoodSummary(

            @AuthenticationPrincipal UserPrincipal principal

    ){

        return ResponseEntity.ok(
                moodTrackingService.getMoodSummary(
                        principal.getEmail()
                )
        );
    }



    // Calendar History
    // GET /api/moods/history?year=2024&month=5

    @GetMapping("/history")
    public ResponseEntity<List<DailyMoodResponse>> getMonthlyHistory(

            @AuthenticationPrincipal UserPrincipal principal,

            @RequestParam int year,

            @RequestParam int month

    ){

        return ResponseEntity.ok(
                moodTrackingService.getMoodHistory(
                        principal.getEmail(),
                        year,
                        month
                )
        );
    }




    // Single Date
    // GET /api/moods/date/2024-05-12

    @GetMapping("/date/{date}")
    public ResponseEntity<DailyMoodResponse> getMoodByDate(

            @AuthenticationPrincipal UserPrincipal principal,

            @PathVariable LocalDate date

    ){

        return ResponseEntity.ok(
                moodTrackingService.getMoodByDate(
                        principal.getEmail(),
                        date
                )
        );
    }




    // Weekly Records

    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyMood(

            @AuthenticationPrincipal UserPrincipal principal

    ){

        return ResponseEntity.ok(
                moodTrackingService.findWeeklyByStatus(
                        principal.getEmail()
                )
        );
    }





    // Monthly Records

    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyMood(

            @AuthenticationPrincipal UserPrincipal principal

    ){

        return ResponseEntity.ok(
                moodTrackingService.findMonthlyStatus(
                        principal.getEmail()
                )
        );
    }





    // Weekly Chart
    // GET /api/moods/summary/week

    @GetMapping("/summary/week")
    public ResponseEntity<WeeklyMoodResponse> getWeeklySummary(

            @AuthenticationPrincipal UserPrincipal principal

    ){

        return ResponseEntity.ok(
                moodTrackingService.getWeeklyMood(
                        principal.getEmail()
                )
        );
    }





    // Delete

    // DELETE /api/moods/delete/1

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteMood(

            @PathVariable Long id,

            @AuthenticationPrincipal UserPrincipal principal

    ){

        moodTrackingService.deleteMood(
                id,
                principal.getEmail()
        );


        return ResponseEntity.noContent().build();
    }


}