package com.mental.controller;

import com.mental.dto.JournalAnalysisResponse;
import com.mental.dto.JournalPhotoResponse;
import com.mental.dto.JournalRequest;
import com.mental.dto.JournalResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.JournalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * REST controller for all Journal-related endpoints.
 *
 * Base path: /api/journals
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  ENDPOINT SUMMARY (Journal Page)                                     │
 * ├────────────────────────────────────┬─────────────────────────────────┤
 * │  POST   /api/journals              │  Create new journal (text only)  │
 * │  GET    /api/journals              │  List journals (all/fav/tagged)  │
 * │  GET    /api/journals/search       │  Search journals by keyword      │
 * │  GET    /api/journals/{id}         │  Get single journal              │
 * │  PUT    /api/journals/{id}         │  Full update (text fields only)  │
 * │  DELETE /api/journals/{id}         │  Delete a journal                │
 * │  PATCH  /api/journals/{id}/favorite│  Toggle favourite flag           │
 * │  PATCH  /api/journals/{id}/private │  Toggle private / lock flag      │
 * │  POST   /api/journals/{id}/photo   │  Upload / replace photo          │
 * │  DELETE /api/journals/{id}/photo   │  Remove attached photo           │
 * │  GET    /api/journals/{id}/analysis│  Get AI analysis result          │
 * │  POST   /api/journals/{id}/analysis│  Trigger AI analysis             │
 * └────────────────────────────────────┴─────────────────────────────────┘
 *
 * Security review:
 *   Ownership validated ✅ | JWT required on all routes ✅
 *   Input validated ✅     | No sensitive data in responses ✅
 *   IDOR protected ✅      | filter param whitelist ✅
 *   Search length-limited ✅
 *   ⚠️ SECURITY FLAG: POST /{id}/analysis has no rate limit.
 *      Add Bucket4j / Spring rate-limit filter before production deployment.
 *      Risk: Medium — malicious user could spam expensive AI analysis calls.
 */
@Validated
@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    /** Allowed values for the ?filter= query parameter. */
    private static final Set<String> ALLOWED_FILTERS = Set.of("all", "favorites", "tagged");

    private final JournalService journalService;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    /**
     * Create a new journal entry.
     *
     * Screen: New Journal Editor (Screen 6)
     *
     * Security review: Ownership from JWT ✅ | @Valid body ✅ | Content encrypted ✅
     */
    @PostMapping
    public ResponseEntity<JournalResponse> createJournal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JournalRequest request) {

        JournalResponse response = journalService.createJournal(userPrincipal, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ─────────────────────────────────────────────
    // READ – LIST  (Screen 5: Journal List)
    // ─────────────────────────────────────────────

    /**
     * Get all journals for the authenticated user.
     *
     * Query params:
     *   filter = all | favorites | tagged   (default: all)
     *
     * Security review: filter param is whitelist-validated ✅ (rejects unknown values with 400)
     */
    @GetMapping
    public ResponseEntity<List<JournalResponse>> getAllJournals(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "all") String filter) {

        // Whitelist-validate filter to prevent unexpected query branching
        if (!ALLOWED_FILTERS.contains(filter.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid filter value. Allowed: all, favorites, tagged");
        }

        List<JournalResponse> responses = journalService.getAllMyJournals(userPrincipal, filter);
        return ResponseEntity.ok(responses);
    }

    // ─────────────────────────────────────────────
    // READ – SEARCH  (Screen 5: search icon)
    // ─────────────────────────────────────────────

    /**
     * Search the user's journals by title keyword.
     *
     * Query params:
     *   q = search term (1–100 chars)
     *
     * Security review: q is length-limited to prevent oversized LIKE queries ✅
     *                  Query uses @Param binding (no SQL injection) ✅
     */
    @GetMapping("/search")
    public ResponseEntity<List<JournalResponse>> searchJournals(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @Size(min = 1, max = 100, message = "Search query must be 1–100 characters") String q) {

        List<JournalResponse> results = journalService.searchJournals(userPrincipal, q);
        return ResponseEntity.ok(results);
    }

    // ─────────────────────────────────────────────
    // READ – SINGLE
    // ─────────────────────────────────────────────

    /**
     * Get a single journal entry by ID.
     *
     * Security review: Ownership validated in service layer ✅ | 403 on violation ✅
     */
    @GetMapping("/{id}")
    public ResponseEntity<JournalResponse> getJournalById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JournalResponse response = journalService.getJournalById(id, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // UPDATE – FULL  (Screen 6: save / edit)
    // ─────────────────────────────────────────────

    /**
     * Fully update an existing journal entry.
     *
     * Security review: Ownership validated ✅ | Content re-encrypted ✅ | @Valid ✅
     */
    @PutMapping("/{id}")
    public ResponseEntity<JournalResponse> updateJournal(
            @PathVariable Long id,
            @Valid @RequestBody JournalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JournalResponse response = journalService.updateJournal(id, request, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────

    /**
     * Permanently delete a journal entry.
     *
     * Security review: Ownership validated ✅ | Returns 204 (no body leakage) ✅
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        journalService.deleteJournal(id, userPrincipal);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // PATCH – TOGGLE FAVOURITE  (Screen 5: heart/star icon)
    // ─────────────────────────────────────────────

    /**
     * Toggle the favourite flag for a journal entry.
     * No request body required.
     *
     * Security review: Ownership validated ✅ | No input to sanitise ✅
     */
    @PatchMapping("/{id}/favorite")
    public ResponseEntity<JournalResponse> toggleFavourite(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JournalResponse response = journalService.toggleFavourite(id, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // PATCH – TOGGLE PRIVATE  (Screen 6: lock / private toggle)
    // ─────────────────────────────────────────────

    /**
     * Toggle the private (lock) flag for a journal entry.
     * No request body required.
     *
     * Security review: Ownership validated ✅ | No input to sanitise ✅
     */
    @PatchMapping("/{id}/private")
    public ResponseEntity<JournalResponse> togglePrivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JournalResponse response = journalService.togglePrivate(id, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // PHOTO – UPLOAD
    // ─────────────────────────────────────────────

    /**
     * Upload or replace a photo attached to a journal entry.
     *
     * Method:       POST
     * Content-Type: multipart/form-data
     * Form field:   "photo" = image file (JPEG / PNG / WebP / GIF, max 5 MB)
     *
     * Example:
     *   POST /api/journals/42/photo
     *   (multipart body with field name "photo")
     *
     * Response 200 OK:
     * {
     *   "journalId": 42,
     *   "photoUrl":  "http://localhost:8080/uploads/journal-photos/uuid.jpg",
     *   "message":   "Photo uploaded successfully"
     * }
     *
     * Security review:
     *   Ownership validated ✅ | MIME type whitelist ✅ | 5 MB size cap ✅
     *   UUID filename (path traversal proof) ✅
     *   ⚠️ SECURITY FLAG: Local disk storage. Migrate to cloud bucket for production.
     */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JournalPhotoResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JournalPhotoResponse response = journalService.uploadPhoto(id, photo, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // PHOTO – DELETE
    // ─────────────────────────────────────────────

    /**
     * Remove the photo attached to a journal entry.
     *
     * Returns 404 if the journal has no photo.
     *
     * Example:
     *   DELETE /api/journals/42/photo
     *
     * Security review:
     *   Ownership validated ✅ | Physical file removed from disk ✅ | No input to sanitise ✅
     */
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<JournalPhotoResponse> deletePhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JournalPhotoResponse response = journalService.deletePhoto(id, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // ANALYSIS – GET  (Screen 7: Journal Analysis)
    // ─────────────────────────────────────────────

    /**
     * Retrieve the existing AI analysis for a journal entry.
     *
     * Security review: Ownership validated ✅ | 404 message sanitised (no path hints) ✅
     */
    @GetMapping("/{id}/analysis")
    public ResponseEntity<JournalAnalysisResponse> getAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JournalAnalysisResponse response = journalService.getAnalysis(id, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // ANALYSIS – TRIGGER  (Screen 7: "Analyse" button)
    // ─────────────────────────────────────────────

    /**
     * Trigger (or re-trigger) AI analysis for a journal entry.
     *
     * Security review: Ownership validated ✅
     * ⚠️ SECURITY FLAG: No rate limit applied.
     *    Add Bucket4j / Spring rate-limit filter before production.
     *    Risk: Medium — a malicious user could spam AI analysis calls,
     *    causing excessive compute cost or service degradation.
     */
    @PostMapping("/{id}/analysis")
    public ResponseEntity<JournalAnalysisResponse> triggerAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JournalAnalysisResponse response = journalService.triggerAnalysis(id, userPrincipal);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
