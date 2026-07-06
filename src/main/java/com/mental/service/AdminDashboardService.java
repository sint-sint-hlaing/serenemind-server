package com.mental.service;

import com.mental.dto.JournalDto;
import com.mental.dto.UserDto;
import com.mental.dto.mood.AuditLogDto;
import com.mental.dto.mood.DashboardStatsDto;
import com.mental.dto.mood.MoodDistributionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminDashboardService {
    DashboardStatsDto getDashboardOverview();
    Page<UserDto> getAllUsers(Pageable pageable);
    List<MoodDistributionDto> getMoodDistributionData();
    Page<AuditLogDto> getAuditLogs(Pageable pageable);
    Page<JournalDto> getFlaggedJournals(Pageable pageable);
    void resolveFlaggedJournal(Long journalId);
}
