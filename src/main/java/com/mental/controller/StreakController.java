package com.mental.controller;

import com.mental.dto.Streak.StreakResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streaks")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;


    @GetMapping("/me")
    public ResponseEntity<StreakResponse> getMyStreak(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(streakService.getStreakDetails(userPrincipal.getEmail()));
    }


    @PostMapping("/use-freeze")
    public ResponseEntity<StreakResponse> useFreeze(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(streakService.useStreakFreeze(userPrincipal.getEmail()));
    }
}
