// GoalController.java (Your existing controller is complete)
package com.mental.controller;

import com.mental.dto.goal.*;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.security.UserPrincipal;
import com.mental.service.UserGoalService;
import com.mental.service.impl.UserGoalServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Goal management endpoints")
public class GoalController {

    private final UserGoalService goalService;

    // ===== CREATE =====
    @Operation(summary = "Create a new goal")
    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GoalRequest request) {
        log.info("Creating goal for user: {}", principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.createGoal(principal.getEmail(), request));
    }

    // ===== GET ALL GOALS =====
    @Operation(summary = "Get all goals for current user")
    @GetMapping
    public ResponseEntity<List<GoalResponse>> getAllGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching all goals for user: {}", principal.getEmail());
        return ResponseEntity.ok(goalService.getUserGoals(principal.getEmail()));
    }

    // ===== GET ACTIVE GOALS =====
    @Operation(summary = "Get active goals (ACTIVE and PAUSED)")
    @GetMapping("/active")
    public ResponseEntity<List<GoalResponse>> getActiveGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching active goals for user: {}", principal.getEmail());
        return ResponseEntity.ok(goalService.getActiveGoals(principal.getEmail()));
    }

    // ===== GET COMPLETED GOALS =====
    @Operation(summary = "Get completed goals")
    @GetMapping("/completed")
    public ResponseEntity<List<GoalResponse>> getCompletedGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching completed goals for user: {}", principal.getEmail());
        return ResponseEntity.ok(goalService.getCompletedGoals(principal.getEmail()));
    }

    // ===== GET GOALS BY STATUS =====
    @Operation(summary = "Get goals by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<GoalResponse>> getGoalsByStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable GoalStatus status) {
        log.debug("Fetching goals for user: {} with status: {}", principal.getEmail(), status);
        return ResponseEntity.ok(goalService.getGoalsByStatus(principal.getEmail(), status));
    }

    // ===== GET STATISTICS =====
    @Operation(summary = "Get goal statistics")
    @GetMapping("/statistics")
    public ResponseEntity<GoalStatistics> getStatistics(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching goal statistics for user: {}", principal.getEmail());
        return ResponseEntity.ok(goalService.getGoalStatistics(principal.getEmail()));
    }

    // ===== UPDATE PROGRESS =====
    @Operation(summary = "Update goal progress (increment by 1)")
    @PatchMapping("/{id}/progress")
    public ResponseEntity<GoalResponse> updateProgress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Updating progress for goal: {} by user: {}", id, principal.getEmail());
        return ResponseEntity.ok(goalService.updateProgress(id, principal.getEmail()));
    }

    // ===== COMPLETE GOAL =====
    @Operation(summary = "Complete a goal")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<GoalResponse> completeGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Completing goal: {} by user: {}", id, principal.getEmail());
        return ResponseEntity.ok(goalService.completeGoal(id, principal.getEmail()));
    }

    // ===== PAUSE GOAL =====
    @Operation(summary = "Pause a goal")
    @PatchMapping("/{id}/pause")
    public ResponseEntity<GoalResponse> pauseGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Pausing goal: {} by user: {}", id, principal.getEmail());
        return ResponseEntity.ok(goalService.pauseGoal(id, principal.getEmail()));
    }

    // ===== RESUME GOAL =====
    @Operation(summary = "Resume a paused goal")
    @PatchMapping("/{id}/resume")
    public ResponseEntity<GoalResponse> resumeGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Resuming goal: {} by user: {}", id, principal.getEmail());
        return ResponseEntity.ok(goalService.resumeGoal(id, principal.getEmail()));
    }

    // ===== DELETE (Soft Delete) =====
    @Operation(summary = "Delete a goal (soft delete - archive)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Deleting goal: {} by user: {}", id, principal.getEmail());
        goalService.deleteGoal(id, principal.getEmail());
        return ResponseEntity.noContent().build();
    }

    // ===== HARD DELETE =====
    @Operation(summary = "Hard delete a goal (permanent)")
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Hard deleting goal: {} by user: {}", id, principal.getEmail());
        goalService.hardDeleteGoal(id, principal.getEmail());
        return ResponseEntity.noContent().build();
    }

    // ===== NOTE MANAGEMENT =====
    @Operation(summary = "Add a note to a goal")
    @PostMapping("/{id}/notes")
    public ResponseEntity<GoalNoteDTO> addNote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GoalNoteRequest request) {
        log.info("Adding note to goal: {} by user: {}", id, principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.addNote(id, principal.getEmail(), request.getContent()));
    }

    @Operation(summary = "Get all notes for a goal")
    @GetMapping("/{id}/notes")
    public ResponseEntity<List<GoalNoteDTO>> getNotes(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching notes for goal: {} by user: {}", id, principal.getEmail());
        return ResponseEntity.ok(goalService.getNotesByGoal(id, principal.getEmail()));
    }

    @Operation(summary = "Update a note")
    @PutMapping("/notes/{noteId}")
    public ResponseEntity<GoalNoteDTO> updateNote(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody NoteUpdateRequest request) {
        log.info("Updating note: {} by user: {}", noteId, principal.getEmail());
        return ResponseEntity.ok(goalService.updateNote(noteId, principal.getEmail(), request.getContent()));
    }

    @Operation(summary = "Delete a note")
    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Deleting note: {} by user: {}", noteId, principal.getEmail());
        goalService.deleteNote(noteId, principal.getEmail());
        return ResponseEntity.noContent().build();
    }

    // ===== UI SPECIFIC =====
    @Operation(summary = "Get goals for dashboard (home screen)")
    @GetMapping("/dashboard")
    public ResponseEntity<List<GoalResponse>> getDashboardGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching dashboard goals for user: {}", principal.getEmail());
        return ResponseEntity.ok(goalService.getGoalsForDashboard(principal.getEmail()));
    }

    @Operation(summary = "Get home screen data")
    @GetMapping("/home")
    public ResponseEntity<HomeResponse> getHomeData(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching home data for user: {}", principal.getEmail());

        List<GoalResponse> goals = goalService.getGoalsForDashboard(principal.getEmail());
        GoalStatistics stats = goalService.getGoalStatistics(principal.getEmail());

        HomeResponse response = new HomeResponse();
        response.setGoals(goals);
        response.setTotalGoals((int) stats.getTotal());
        response.setActiveGoals((int) stats.getActive());
        response.setCompletedGoals((int) stats.getCompleted());
        response.setTotalStreak(stats.getTotalStreak());
        response.setCurrentStreak(stats.getCurrentStreak());

        return ResponseEntity.ok(response);
    }
}