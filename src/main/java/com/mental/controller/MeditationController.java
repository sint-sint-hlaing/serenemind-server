package com.mental.controller;

import com.mental.dto.meditation.MeditationDashboardResponse;
import com.mental.dto.MeditationResponse;
import com.mental.dto.MeditationSessionRequest;
import com.mental.service.MeditationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meditation")
@RequiredArgsConstructor
public class MeditationController {

    private final MeditationService meditationService;

    @GetMapping("/dashboard")
    public ResponseEntity<MeditationDashboardResponse> dashboard() {
        return ResponseEntity.ok(meditationService.getDashboard());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeditationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(meditationService.getById(id));
    }

    @PostMapping("/complete")
    public ResponseEntity<String> completeSession(
            @RequestBody MeditationSessionRequest request) {

        meditationService.completeSession(request);
        return ResponseEntity.ok("Completed");
    }

    @GetMapping("/history")
    public ResponseEntity<List<MeditationResponse>> history() {
        return ResponseEntity.ok(meditationService.getHistory());
    }
}