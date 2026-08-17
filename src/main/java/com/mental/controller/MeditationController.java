package com.mental.controller;

import com.mental.dto.meditation.MeditationSessionRequest;
import com.mental.dto.meditation.*;
import com.mental.model.entity.enums.MeditationCategory;
import com.mental.model.entity.enums.MeditationTime;
import com.mental.security.UserPrincipal;
import com.mental.service.MeditationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<MeditationResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        log.debug("Fetching meditation by id: {}", id);
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(meditationService.getById(id,userId));
    }

    @Operation(summary = "Get all meditations or filter by category")
    @GetMapping
    public ResponseEntity<List<MeditationResponse>> getAll(
            @RequestParam(required = false) MeditationCategory category) {

        if (category != null) {
            log.debug("Fetching meditations by category: {}", category);
            return ResponseEntity.ok(meditationService.getByCategory(category));
        }

        log.debug("Fetching all meditations");
        return ResponseEntity.ok(meditationService.getAll(category));
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
    public ResponseEntity<List<MeditationHistoryResponse>> history(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching meditation history for user: {}", principal.getEmail());
        return ResponseEntity.ok(meditationService.getHistory(principal.getEmail()));
    }

    @Operation(summary = "Get meditation statistics")
    @GetMapping("/statistics")
    public ResponseEntity<MeditationStatistics> getStatistics() {
        return ResponseEntity.ok(
                meditationService.getStatistics()
        );
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable Long id) {

        String url = meditationService.getAudioUrl(id);

        return ResponseEntity.ok(
                Map.of("downloadUrl", url)
        );
    }
    @PostMapping("/{id}/favorite")
    public ResponseEntity<FavoriteResponse> toggleFavorite(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                meditationService.toggleFavorite(principal.getId(), id));
    }

    @Operation(summary = "Get meditations by category")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<MeditationResponse>> getByCategory(
            @PathVariable String category) {

        MeditationCategory meditationCategory =
                MeditationCategory.valueOf(category.toUpperCase());

        return ResponseEntity.ok(
                meditationService.getByCategory(meditationCategory)
        );
    }

    @Operation(summary = "Get meditation audio URL")
    @GetMapping("/{id}/audio")
    public ResponseEntity<String> getAudioUrl(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                meditationService.getAudioUrl(id)
        );
    }

    @Operation(summary = "Get meditations by time")
    @GetMapping("/time/{time}")
    public ResponseEntity<List<MeditationResponse>> getByTime(
            @PathVariable String time) {

        MeditationTime meditationTime =
                MeditationTime.valueOf(time.toUpperCase());

        return ResponseEntity.ok(
                meditationService.getByTime(meditationTime)
        );
    }
    @Operation(summary = "Search meditations")
    @GetMapping("/search")
    public ResponseEntity<List<MeditationResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String time) {

        MeditationCategory meditationCategory = null;
        MeditationTime meditationTime = null;

        if (category != null && !category.isBlank()) {
            meditationCategory =
                    MeditationCategory.valueOf(category.toUpperCase());
        }

        if (time != null && !time.isBlank()) {
            meditationTime =
                    MeditationTime.valueOf(time.toUpperCase());
        }

        log.debug(
                "Searching meditations. query={}, category={}, time={}",
                query, meditationCategory, meditationTime
        );

        return ResponseEntity.ok(
                meditationService.search(
                        query,
                        meditationCategory,
                        meditationTime
                )
        );
    }

    @PostMapping("/{id}/timer")
    public ResponseEntity<TimerResponse> saveTimer(
            @PathVariable Long id,
            @Valid @RequestBody TimerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                meditationService.saveTimer(
                        principal.getId(),
                        id,
                        request));
    }

    @GetMapping("/{id}/share")
    public ResponseEntity<ShareResponse> shareMeditation(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                meditationService.getShareLink(id));
    }

    @GetMapping("/{id}/previous")
    public ResponseEntity<MeditationResponse> previous(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                meditationService.getPrevious(id));
    }

    @GetMapping("/{id}/next")
    public ResponseEntity<MeditationList> next(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                meditationService.getNext(id));
    }



}



