package com.mental.mapper;

import com.mental.dto.report.ReportDto;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public ReportDto toDto(long totalReports, long todayReports) {
        return ReportDto.builder()
                .reportedBy("COMMUNITY_REPORT")
                .total(totalReports)
                .todayCount(todayReports)
                .growthPercentage(calculateGrowthPercentage(totalReports, todayReports))
                .build();
    }

    private double calculateGrowthPercentage(long total, long today) {
        if (total == 0) {
            return 0.0;
        }
        return (today * 100.0) / total;
    }
}