package com.mental.service.admin.impl;

import com.mental.dto.report.ReportDto;
import com.mental.dto.report.ReportSummaryDto;
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
    public ReportSummaryDto userReport() {

        long totalUsers =
                userRepository.count();


        long todayUsers =
                userRepository.countByCreatedAtBetween(
                        LocalDateTime.now()
                                .toLocalDate()
                                .atStartOfDay(),
                        LocalDateTime.now()
                );


        return ReportSummaryDto.builder()
                .id(1L) // လိုအပ်သော ID (သို့မဟုတ် Database မှ ရလာသော ID)
                .reportType("USER_REPORT")
                .total(totalUsers)
                .todayCount(todayUsers)
                .growthPercentage(0.0)
                .generatedAt(LocalDateTime.now())
                .build();
    }


    @Override
    public ReportSummaryDto moodReport() {


        long totalMood =
                moodRepository.count();


        long todayMood =
                moodRepository.countToday();
        return ReportSummaryDto.builder()
                .id(1L) // လိုအပ်သော ID (သို့မဟုတ် Database မှ ရလာသော ID)
                .reportType("MOOD_REPORT")
                .total(totalMood)
                .todayCount(todayMood)
                .growthPercentage(0.0)
                .generatedAt(LocalDateTime.now())
                .build();


    }



    @Override
    public ReportSummaryDto meditationReport() {


        long totalSession =
                meditationRepository.count();


        long todaySession =
                meditationRepository.countToday();


        return ReportSummaryDto.builder()
                .id(1L) // လိုအပ်သော ID (သို့မဟုတ် Database မှ ရလာသော ID)
                .reportType("MEDITATION_REPORT")
                .total(todaySession)
                .todayCount(totalSession)
                .growthPercentage(0.0)
                .generatedAt(LocalDateTime.now())
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