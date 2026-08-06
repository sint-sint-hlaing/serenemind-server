package com.mental.dto.report;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Builder
public record ReportDto(
        Long id,
        Long postId,
        String reportedBy,
        String reason,
        String status,
        long total,
        long todayCount,
        double growthPercentage,
        LocalDateTime createdAt
) { }