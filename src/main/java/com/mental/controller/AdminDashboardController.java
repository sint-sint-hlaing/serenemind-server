package com.mental.controller;

import com.mental.dto.JournalDto;
import com.mental.dto.UserDto;
import com.mental.dto.mood.AuditLogDto;
import com.mental.dto.mood.DashboardStatsDto;
import com.mental.dto.mood.MoodDistributionDto;
import com.mental.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final AdminDashboardService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardOverview());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<MoodDistributionDto>> getMoodReports() {
        return ResponseEntity.ok(adminService.getMoodDistributionData());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAuditLogs(pageable));
    }

    @GetMapping("/journals/flagged")
    public ResponseEntity<Page<JournalDto>> getFlaggedJournals(Pageable pageable) {
        return ResponseEntity.ok(adminService.getFlaggedJournals(pageable));
    }
    @PutMapping("/journals/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resolveJournal(@PathVariable Long id) {
        adminService.resolveFlaggedJournal(id);
        return ResponseEntity.ok("Journal is solved");
    }
}