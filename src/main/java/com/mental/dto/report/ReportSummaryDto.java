package com.mental.dto.report;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ReportSummaryDto(
        Long id,
        String reportType,
        long total,
        long todayCount,
        double growthPercentage,
        LocalDateTime generatedAt
) { }