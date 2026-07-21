package com.mental.mapper;

import com.mental.dto.mood.AuditLogDto;
import com.mental.dto.mood.MoodDistributionDto;
import com.mental.model.entity.AuditLog;
import com.mental.model.entity.enums.MoodType;
import org.springframework.stereotype.Component;

@Component
public class DashboardStatsMapper {

    public MoodDistributionDto toMoodDistributionDto(Object[] row, long total) {
        MoodType mood = (MoodType) row[0];
        long count = (long) row[1];
        double percentage = (count * 100.0) / total;

        return new MoodDistributionDto(mood, count, percentage);
    }

    public AuditLogDto toAuditLogDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUsername(),
                log.getAction(),
                log.getTargetId(),
                log.getDescription(),
                log.getCreatedAt()
        );
    }
}