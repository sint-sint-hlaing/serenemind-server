package com.mental.controller;

import com.mental.dto.JournalDto;
import com.mental.dto.UserDto;
import com.mental.dto.dashboard.*;
import com.mental.dto.mood.AuditLogDto;
import com.mental.dto.mood.DashboardStatsDto;
import com.mental.dto.mood.MoodDistributionDto;
import com.mental.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "Get all users with pagination")
    @GetMapping("/summary/users")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Fetching all users with pagination");
        return ResponseEntity.ok(adminDashboardService.getAllUsers(pageable));
    }

    @Operation(summary = "Get active users with pagination")
    @GetMapping("/summary/users/active")
    public ResponseEntity<Page<UserDto>> getActiveUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Fetching active users with pagination");
        return ResponseEntity.ok(adminDashboardService.getActiveUsers(pageable));
    }


    @Operation(summary = "Get mood distribution data")
    @GetMapping("/summary/reports")
    public ResponseEntity<List<MoodDistributionDto>> getMoodReports() {
        log.info("Fetching mood distribution data");
        return ResponseEntity.ok(adminDashboardService.getMoodDistributionData());
    }


    @Operation(summary = "Get mood statistics")
    @GetMapping("/summary/moods")
    public ResponseEntity<MoodStatsResponse> getMoodsCount() {
        log.info("Fetching mood statistics");
        return ResponseEntity.ok(adminDashboardService.getMoodsCount());
    }
    @Operation(summary = "Get journals statistics")
    @GetMapping("/summary/journals")
    public ResponseEntity<JournalStatsResponse> getJournalsCount() {
        log.info("Fetching journals statistics");
        return ResponseEntity.ok(adminDashboardService.getJournalsCount());
    }

    @Operation(summary = "Get meditations statistics")
    @GetMapping("/summary/meditations")
    public ResponseEntity<MeditationStatsResponse> getMeditationsCount() {
        log.info("Fetching meditations statistics");
        return ResponseEntity.ok(adminDashboardService.getMeditationsCount());
    }

    @Operation(summary = "Get growth rate")
    @GetMapping("/summary/growth")
    public ResponseEntity<GrowthRateResponse> getGrowthRate() {
        log.info("Fetching growth rate");
        return ResponseEntity.ok(adminDashboardService.getMonthlyRate());
    }

    @Operation(summary = "Get audit logs with pagination")
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Fetching audit logs with pagination");
        return ResponseEntity.ok(adminDashboardService.getAuditLogs(pageable));
    }

    @Operation(summary = "Get flagged journals with pagination")
    @GetMapping("/journals/flagged")
    public ResponseEntity<Page<FlaggedJournalResponse>> getFlaggedJournals(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Fetching flagged journals with pagination");
        return ResponseEntity.ok(adminDashboardService.getFlaggedJournals(pageable));
    }

    @Operation(summary = "Resolve a flagged journal")
    @PutMapping("/journals/{id}/resolve")
    public ResponseEntity<ActionResponse> resolveJournal(@PathVariable @Min(1) Long id) {
        log.info("Resolving flagged journal with id: {}", id);
        adminDashboardService.resolveFlaggedJournal(id);
        return ResponseEntity.ok().build();
    }

}