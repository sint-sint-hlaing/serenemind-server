package com.mental.service;

import com.mental.dto.JournalDto;
import com.mental.dto.UserDto;
import com.mental.dto.dashboard.*;
import com.mental.dto.mood.AuditLogDto;
import com.mental.dto.mood.DashboardStatsDto;
import com.mental.dto.mood.MoodDistributionDto;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminDashboardService {
   // DashboardStatsDto getDashboardOverview();
    Page<UserDto> getAllUsers(Pageable pageable);


 Page<UserDto> getActiveUsers(Pageable pageable);

 List<MoodDistributionDto> getMoodDistributionData();

 MoodStatsResponse getMoodsCount();

 JournalStatsResponse getJournalsCount();

 MeditationStatsResponse getMeditationsCount();

 GrowthRateResponse getMonthlyRate();

 Page<AuditLogResponse> getAuditLogs(Pageable pageable);

 Page<FlaggedJournalResponse> getFlaggedJournals(Pageable pageable);

 void resolveFlaggedJournal(@Min(1) Long id);
}
