package com.mental.dto.report;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ReportSummaryDto(
        Long id,
        String reportType,      // 👈 ဘယ် Report အမျိုးအစားလဲ (ဥပမာ: MOOD_REPORT)
        long total,
        long todayCount,
        double growthPercentage,
        LocalDateTime generatedAt // 👈 Report ထုတ်ပေးသည့်အချိန်
) { }