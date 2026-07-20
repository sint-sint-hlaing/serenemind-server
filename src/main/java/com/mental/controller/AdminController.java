package com.mental.controller;

import com.mental.dto.Notification.NotificationRequest;
import com.mental.dto.Post.PostResponse;
import com.mental.dto.UserDto;
import com.mental.dto.admin.*;
import com.mental.dto.goal.UserGoal;
import com.mental.dto.meditation.MeditationRequest;
import com.mental.dto.report.ReportDto;
import com.mental.service.admin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminJournalService adminJournalService;
    private final AdminMeditationService adminMeditationService;
    private final AdminGoalService adminGoalService;
    private final AdminNotificationService adminNotificationService;
    private final AdminCommunityService adminCommunityService;
    private final AdminReportService adminReportService;

    // ================= USER MANAGEMENT =================

    @Operation(summary = "Get all users")
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        log.info("Fetching all users");
        return ResponseEntity.ok(adminUserService.getUsers());
    }

    @Operation(summary = "Block a user")
    @PutMapping("/users/{id}/block")
    public ResponseEntity<UserDto> blockUser(@PathVariable Long id) {
        log.info("Blocking user with id: {}", id);
        return ResponseEntity.ok(adminUserService.blockUser(id));
    }

    @Operation(summary = "Activate a user")
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<UserDto> activateUser(@PathVariable Long id) {
        log.info("Activating user with id: {}", id);
        return ResponseEntity.ok(adminUserService.activateUser(id));
    }

    // ================= JOURNAL MANAGEMENT =================

    @Operation(summary = "Delete a journal")
    @DeleteMapping("/journals/{id}")
    public ResponseEntity<Void> deleteJournal(@PathVariable Long id) {
        log.info("Deleting journal with id: {}", id);
        adminJournalService.deleteJournal(id);
        return ResponseEntity.noContent().build();
    }

    // ================= MEDITATION MANAGEMENT =================

    @Operation(summary = "Create a new meditation")
    @PostMapping("/meditations")
    public ResponseEntity<MeditationAdminDto> createMeditation(
            @Valid @RequestBody MeditationRequest request) {
        log.info("Creating new meditation: {}", request.title());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminMeditationService.createMeditation(request));
    }

    // ================= GOAL MANAGEMENT =================

    @Operation(summary = "Get all goals")
    @GetMapping("/goals")
    public ResponseEntity<List<UserGoal>> getGoals() {
        log.info("Fetching all goals");
        return ResponseEntity.ok(adminGoalService.getGoals());
    }

    @Operation(summary = "Get goal statistics")
    @GetMapping("/goals/statistics")
    public ResponseEntity<GoalStatisticDto> getGoalStatistics() {
        log.info("Fetching goal statistics");
        return ResponseEntity.ok(adminGoalService.getStatistics());
    }

    // ================= NOTIFICATION MANAGEMENT =================

    @Operation(summary = "Create a notification")
    @PostMapping("/notifications")
    public ResponseEntity<NotificationDto> createNotification(
            @Valid @RequestBody NotificationRequest request) {
        log.info("Creating notification: {}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminNotificationService.createNotification(request));
    }

    // ================= COMMUNITY MANAGEMENT =================

    @Operation(summary = "Get all community posts")
    @GetMapping("/community/posts")
    public ResponseEntity<List<PostResponse>> getCommunityPosts() {
        log.info("Fetching all community posts");
        return ResponseEntity.ok(adminCommunityService.getCommunityPosts());
    }

    @Operation(summary = "Delete a community post")
    @DeleteMapping("/community/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        log.info("Deleting community post with id: {}", id);
        adminCommunityService.deleteCommunityPost(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get community reports")
    @GetMapping("/community/reports")
    public ResponseEntity<List<ReportDto>> getCommunityReports() {
        log.info("Fetching community reports");
        return ResponseEntity.ok(adminCommunityService.getReports());
    }

    // ================= REPORT MANAGEMENT =================

    @Operation(summary = "Get user report")
    @GetMapping("/reports/users")
    public ResponseEntity<ReportDto> userReport() {
        log.info("Fetching user report");
        return ResponseEntity.ok(adminReportService.userReport());
    }

    @Operation(summary = "Get mood report")
    @GetMapping("/reports/mood")
    public ResponseEntity<ReportDto> moodReport() {
        log.info("Fetching mood report");
        return ResponseEntity.ok(adminReportService.moodReport());
    }

    @Operation(summary = "Get meditation report")
    @GetMapping("/reports/meditation")
    public ResponseEntity<ReportDto> meditationReport() {
        log.info("Fetching meditation report");
        return ResponseEntity.ok(adminReportService.meditationReport());
    }
}