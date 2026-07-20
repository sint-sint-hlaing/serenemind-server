package com.mental.controller;

import com.mental.dto.MeditationSessionRequest;
import com.mental.dto.meditation.MeditationDashboardResponse;
import com.mental.dto.meditation.MeditationResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.MeditationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meditations")
@RequiredArgsConstructor
@Tag(name = "Meditation", description = "Meditation management endpoints")
public class MeditationController {

    private final MeditationService meditationService;

    @Operation(summary = "Get meditation dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<MeditationDashboardResponse> dashboard() {
        log.debug("Fetching meditation dashboard");
        return ResponseEntity.ok(meditationService.getDashboard());
    }

    @Operation(summary = "Get meditation by ID")
    @GetMapping("/{id}")
    public ResponseEntity<MeditationResponse> getById(@PathVariable Long id) {
        log.debug("Fetching meditation by id: {}", id);
        return ResponseEntity.ok(meditationService.getById(id));
    }

    @Operation(summary = "Complete a meditation session")
    @PostMapping("/complete")
    public ResponseEntity<Void> completeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MeditationSessionRequest request) {
        log.info("Completing meditation session for user: {}", principal.getEmail());
        meditationService.completeSession(principal.getEmail(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get meditation history")
    @GetMapping("/history")
    public ResponseEntity<List<MeditationResponse>> history(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching meditation history for user: {}", principal.getEmail());
        return ResponseEntity.ok(meditationService.getHistory(principal.getEmail()));
    }
}