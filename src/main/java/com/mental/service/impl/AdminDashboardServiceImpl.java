package com.mental.service.impl;

import com.mental.dto.JournalDto;
import com.mental.dto.UserDto;
import com.mental.dto.dashboard.*;
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
    @Cacheable("moodStats")
    public MoodStatsResponse getMoodsCount() {

        log.info("Fetching mood statistics");


        long totalEntries = moodRepository.count();


        String mostCommonMood =
                moodRepository.findMostCommonMood();


        Double averageScore =
                moodRepository.findAverageScore();


        return new MoodStatsResponse(

                totalEntries,

                mostCommonMood != null
                        ? mostCommonMood
                        : "N/A",

                averageScore != null
                        ? averageScore
                        : 0.0

        );
    }

    @Override
    public JournalStatsResponse getJournalsCount() {


        log.info("Fetching journal statistics");


        long total =
                journalRepository.count();


        long today =
                journalRepository.countToday();



        long flagged =
                journalRepository.countByFlaggedTrue();



        double growth =
                calculateJournalGrowth();



        return new JournalStatsResponse(

                total,

                today,

                flagged,

                growth

        );
    }

    @Override
    public MeditationStatsResponse getMeditationsCount(){

        log.info("Fetching meditation statistics");


        long total =
                meditationRepository.count();


        long completed =
                meditationRepository
                        .countByCompletedTrue();



        long activeUsers =
                meditationRepository
                        .countDistinctUsers();



        double rate = 0;


        if(total > 0){

            rate =
                    (completed *100.0)
                            / total;

        }



        return new MeditationStatsResponse(

                total,

                completed,

                activeUsers,

                rate

        );

    }

    @Override
    @Cacheable("growthRate")
    public GrowthRateResponse getMonthlyRate(){


        log.info("Calculating monthly growth");


        LocalDateTime now =
                LocalDateTime.now();



        long current =
                userRepository
                        .countByCreatedAtBetween(
                                now.minusMonths(1),
                                now
                        );



        long previous =
                userRepository
                        .countByCreatedAtBetween(
                                now.minusMonths(2),
                                now.minusMonths(1)
                        );



        double growth = 0;


        if(previous > 0){

            growth =
                    ((current-previous)
                            *100.0)
                            /previous;

        }



        return new GrowthRateResponse(

                current,

                previous,

                growth

        );

    }

    @Override
    public Page<AuditLogResponse> getAuditLogs(
            Pageable pageable
    ){


        return auditLogRepository
                .findAll(pageable)
                .map(log ->

                        new AuditLogResponse(

                                log.getId(),

                                log.getUsername(),

                                log.getAction(),

                                log.getDescription(),

                                log.getCreatedAt()

                        )

                );

    }

    @Override
    public Page<FlaggedJournalResponse> getFlaggedJournals(
            Pageable pageable
    ){


        return journalRepository
                .findAllByFlaggedTrue(pageable)

                .map(journal ->

                        new FlaggedJournalResponse(

                                journal.getId(),

                                journal.getTitle(),

                                journal.getUser().getId(),

                                journal.getFlagReason(),

                                journal.isFlagged(),

                                journal.getCreatedAt()

                        )

                );

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


    private double calculateJournalGrowth(){


        LocalDateTime now =
                LocalDateTime.now();


        long current =
                journalRepository.countByCreatedAtBetween(
                        now.minusMonths(1),
                        now
                );


        long previous =
                journalRepository.countByCreatedAtBetween(
                        now.minusMonths(2),
                        now.minusMonths(1)
                );


        if(previous == 0)
            return 0;


        return ((current-previous)
                *100.0)
                / previous;

    }
}