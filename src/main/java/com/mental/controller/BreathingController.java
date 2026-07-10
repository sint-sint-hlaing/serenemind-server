package com.mental.controller;


import com.mental.dto.Breathing.BreathingSessionRequest;
import com.mental.dto.Breathing.BreathingSessionResponse;
import com.mental.dto.Breathing.BreathingSummaryResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.BreathingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/breathing")
@RequiredArgsConstructor
public class BreathingController {

    private final BreathingService breathingService;

    // UI - "Start Breathing" button click on Configuration Screen
    @PostMapping("/session/start")
    public ResponseEntity<BreathingSessionResponse> startSession(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid BreathingSessionRequest request) {

        BreathingSessionResponse response = breathingService.startBreathingSession(userPrincipal, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // UI - Event tracking for completed timers/rounds if tracked real-time on server
    @PostMapping("/session/{sessionId}/round-complete")
    public ResponseEntity<Void> trackRoundCompletion(
            @PathVariable String sessionId,
            @RequestParam int roundNumber) {

        breathingService.logRoundCompletion(sessionId, roundNumber);
        return ResponseEntity.ok().build();
    }

    // UI - "Done" button or completion transition to show the Summary screen
    @PostMapping("/session/{sessionId}/complete")
    public ResponseEntity<BreathingSummaryResponse> completeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        BreathingSummaryResponse summary = breathingService.completeBreathingSession(sessionId, userPrincipal);
        return ResponseEntity.ok(summary);
    }
}
