package com.mental.service.admin.impl;

import com.mental.dto.report.ReportDto;
import com.mental.repository.MeditationSessionRepository;
import com.mental.repository.MoodTrackingRepository;
import com.mental.repository.UserRepository;
import com.mental.service.admin.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {


    private final UserRepository userRepository;
    private final MoodTrackingRepository moodRepository;
    private final MeditationSessionRepository meditationRepository;


    @Override
    public ReportDto userReport() {

        long totalUsers =
                userRepository.count();


        long todayUsers =
                userRepository.countByCreatedAtBetween(
                        LocalDateTime.now()
                                .toLocalDate()
                                .atStartOfDay(),
                        LocalDateTime.now()
                );


        return ReportDto.builder()
                .reason("USER_REPORT")
                .total(totalUsers)
                .todayCount(todayUsers)
                .growthPercentage(
                        calculateGrowth(
                                todayUsers,
                                totalUsers
                        )
                )
                .createdAt(LocalDateTime.now())
                .build();
    }


    @Override
    public ReportDto moodReport() {


        long totalMood =
                moodRepository.count();


        long todayMood =
                moodRepository.countToday();


        return ReportDto.builder()
                .reason("MOOD_REPORT")
                .total(totalMood)
                .todayCount(todayMood)
                .growthPercentage(
                        calculateGrowth(
                                todayMood,
                                totalMood
                        )
                )
                .createdAt(LocalDateTime.now())
                .build();

    }



    @Override
    public ReportDto meditationReport() {


        long totalSession =
                meditationRepository.count();


        long todaySession =
                meditationRepository.countToday();


        return ReportDto.builder()
                .reason("MEDITATION_REPORT")
                .total(totalSession)
                .todayCount(todaySession)
                .growthPercentage(
                        calculateGrowth(
                                todaySession,
                                totalSession
                        )
                )
                .createdAt(LocalDateTime.now())
                .build();

    }



    private double calculateGrowth(
            long current,
            long total
    ){

        if(total == 0){
            return 0;
        }

        return (current * 100.0) / total;
    }
}