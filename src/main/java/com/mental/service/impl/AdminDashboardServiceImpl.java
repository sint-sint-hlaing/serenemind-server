package com.mental.service.impl;

import com.mental.dto.JournalDto;
import com.mental.dto.UserDto;
import com.mental.dto.mood.AuditLogDto;
import com.mental.dto.mood.DashboardStatsDto;
import com.mental.dto.mood.MoodDistributionDto;
import com.mental.mapper.DashboardStatsMapper;
import com.mental.mapper.UserMapper;
import com.mental.repository.*;
import com.mental.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final MoodTrackingRepository moodRepository;
    private final JournalRepository journalRepository;
    private final MeditationRepository meditationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserMapper userMapper;
    private final DashboardStatsMapper statsMapper;

    @Override
    @Cacheable(value = "adminUsers", key = "#pageable")
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination: {}", pageable);
        return userRepository.findAll(pageable)
                .map(userMapper::toAdminDto);
    }

    @Override
    @Cacheable(value = "adminActiveUsers", key = "#pageable")
    public Page<UserDto> getActiveUsers(Pageable pageable) {
        log.debug("Fetching active users with pagination: {}", pageable);
        return userRepository.findByIsActiveTrue(pageable)
                .map(userMapper::toAdminDto);
    }

    @Override
    @Cacheable("moodDistribution")
    public List<MoodDistributionDto> getMoodDistributionData() {
        log.debug("Fetching mood distribution data");

        long total = moodRepository.count();
        if (total == 0) {
            return List.of();
        }

        return moodRepository.getMoodDistribution().stream()
                .map(obj -> statsMapper.toMoodDistributionDto(obj, total))
                .collect(Collectors.toList());
    }

    @Override
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        log.debug("Fetching audit logs with pagination: {}", pageable);
        return auditLogRepository.findAll(pageable)
                .map(statsMapper::toAuditLogDto);
    }

    @Override
    public Page<JournalDto> getFlaggedJournals(Pageable pageable) {
        log.debug("Fetching flagged journals with pagination: {}", pageable);
        return journalRepository.findAllByFlaggedTrueOrderByCreatedAtDesc(pageable)
                .map(journal -> JournalDto.builder()
                        .id(journal.getId())
                        .title(journal.getTitle())
                        .content(journal.getContent())
                        .isFlagged(journal.isFlagged())
                        .flagReason(journal.getFlagReason())
                        .createdAt(journal.getCreatedAt())
                                .userId(journal.getUser().getId())
                        .build());
    }

    @Override
    @Transactional
    public void resolveFlaggedJournal(Long journalId) {
        log.info("Resolving flagged journal: {}", journalId);

        journalRepository.findById(journalId)
                .ifPresent(journal -> {
                    journal.setFlagged(false);
                    journal.setFlagReason("Resolved by Admin at " + LocalDateTime.now());
                    journalRepository.save(journal);
                    log.info("Journal {} resolved successfully", journalId);
                });
    }

    @Override
    @Cacheable("growthRate")
    public DashboardStatsDto getMonthlyRate() {
        log.debug("Calculating monthly growth rate");

        LocalDateTime now = LocalDateTime.now();
        long currentMonthUsers = userRepository.countByCreatedAtBetween(
                now.minusMonths(1), now);
        long previousMonthUsers = userRepository.countByCreatedAtBetween(
                now.minusMonths(2), now.minusMonths(1));

        long growth = calculateGrowthPercentage(currentMonthUsers, previousMonthUsers);

        return DashboardStatsDto.of(
                currentMonthUsers,
                growth,
                0,
                0,
                0,
                List.of()
        );
    }

    @Override
    public DashboardStatsDto getMeditationsCount() {
        return DashboardStatsDto.of(
                0, 0, 0, 0,
                meditationRepository.count(),
                List.of()
        );
    }

    @Override
    public DashboardStatsDto getJournalsCount() {
        return DashboardStatsDto.of(
                0, 0, 0,
                journalRepository.count(),
                0,
                List.of()
        );
    }

    @Override
    public DashboardStatsDto getMoodsCount() {
        return DashboardStatsDto.of(
                0, 0,
                moodRepository.count(),
                0,
                0,
                List.of()
        );
    }

    private long calculateGrowthPercentage(long current, long previous) {
        if (previous == 0) {
            return 100;
        }
        return Math.round(((current - previous) * 100.0) / previous);
    }
}