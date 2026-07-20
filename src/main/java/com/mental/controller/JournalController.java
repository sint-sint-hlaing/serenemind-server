package com.mental.controller;

import com.mental.dto.JournalAnalysisResponse;
import com.mental.dto.JournalPhotoResponse;
import com.mental.dto.JournalRequest;
import com.mental.dto.JournalResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.JournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
@Tag(name = "Journal", description = "Journal management endpoints")
public class JournalController {

    private static final Set<String> ALLOWED_FILTERS = Set.of("all", "favorites", "tagged");
    private static final int MAX_SEARCH_LENGTH = 100;
    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5 MB

    private final JournalService journalService;

    @Operation(summary = "Create a new journal entry")
    @PostMapping
    public ResponseEntity<JournalResponse> createJournal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JournalRequest request) {
        log.info("Creating journal for user: {}", userPrincipal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(journalService.createJournal(userPrincipal, request));
    }

    @Operation(summary = "Get all journals")
    @GetMapping
    public ResponseEntity<List<JournalResponse>> getAllJournals(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "all") String filter) {

        if (!ALLOWED_FILTERS.contains(filter.toLowerCase())) {
            throw new IllegalArgumentException("Invalid filter. Allowed: all, favorites, tagged");
        }

        log.debug("Fetching journals for user: {} with filter: {}", userPrincipal.getEmail(), filter);
        return ResponseEntity.ok(journalService.getAllMyJournals(userPrincipal, filter));
    }

    @Operation(summary = "Search journals")
    @GetMapping("/search")
    public ResponseEntity<List<JournalResponse>> searchJournals(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @Size(min = 1, max = MAX_SEARCH_LENGTH) String q) {
        log.debug("Searching journals for user: {} with query: {}", userPrincipal.getEmail(), q);
        return ResponseEntity.ok(journalService.searchJournals(userPrincipal, q));
    }

    @Operation(summary = "Get journal by ID")
    @GetMapping("/{id}")
    public ResponseEntity<JournalResponse> getJournalById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("Fetching journal: {} for user: {}", id, userPrincipal.getEmail());
        return ResponseEntity.ok(journalService.getJournalById(id, userPrincipal));
    }

    @Operation(summary = "Update journal")
    @PutMapping("/{id}")
    public ResponseEntity<JournalResponse> updateJournal(
            @PathVariable Long id,
            @Valid @RequestBody JournalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Updating journal: {} for user: {}", id, userPrincipal.getEmail());
        return ResponseEntity.ok(journalService.updateJournal(id, request, userPrincipal));
    }

    @Operation(summary = "Delete journal")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Deleting journal: {} for user: {}", id, userPrincipal.getEmail());
        journalService.deleteJournal(id, userPrincipal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle favorite")
    @PatchMapping("/{id}/favorite")
    public ResponseEntity<JournalResponse> toggleFavorite(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Toggling favorite for journal: {} by user: {}", id, userPrincipal.getEmail());
        return ResponseEntity.ok(journalService.toggleFavourite(id, userPrincipal));
    }

    @Operation(summary = "Toggle private")
    @PatchMapping("/{id}/private")
    public ResponseEntity<JournalResponse> togglePrivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Toggling private for journal: {} by user: {}", id, userPrincipal.getEmail());
        return ResponseEntity.ok(journalService.togglePrivate(id, userPrincipal));
    }

    @Operation(summary = "Upload photo")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JournalPhotoResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        validatePhoto(photo);
        log.info("Uploading photo for journal: {} by user: {}", id, userPrincipal.getEmail());
        return ResponseEntity.ok(journalService.uploadPhoto(id, photo, userPrincipal));
    }

    @Operation(summary = "Delete photo")
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<JournalPhotoResponse> deletePhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Deleting photo for journal: {} by user: {}", id, userPrincipal.getEmail());
        return ResponseEntity.ok(journalService.deletePhoto(id, userPrincipal));
    }

    @Operation(summary = "Get analysis")
    @GetMapping("/{id}/analysis")
    public ResponseEntity<JournalAnalysisResponse> getAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("Fetching analysis for journal: {} by user: {}", id, userPrincipal.getEmail());
        return ResponseEntity.ok(journalService.getAnalysis(id, userPrincipal));
    }

    @Operation(summary = "Trigger analysis")
    @PostMapping("/{id}/analysis")
    public ResponseEntity<JournalAnalysisResponse> triggerAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Triggering analysis for journal: {} by user: {}", id, userPrincipal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(journalService.triggerAnalysis(id, userPrincipal));
    }

    private void validatePhoto(MultipartFile photo) {
        if (photo.isEmpty()) {
            throw new IllegalArgumentException("Photo file is empty");
        }

        if (photo.getSize() > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException("Photo size exceeds 5MB limit");
        }

        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file type. Only images are allowed");
        }
    }
}