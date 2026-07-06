package com.mental.mapper;

import com.mental.dto.mood.MoodEntryDto;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.enums.MoodType;
import org.springframework.stereotype.Component;

@Component
public class MoodMapper {

    public MoodEntryDto toDto(MoodEntry entry) {
        MoodEntryDto dto = new MoodEntryDto();
        dto.setMoodType(entry.getMood().name());
        dto.setIntensity(entry.getIntensity());
        dto.setNote(entry.getNote());
        if (entry.getAnalysis() != null) {
            dto.setEmotion(entry.getAnalysis().getEmotion());
            dto.setStressLevel(entry.getAnalysis().getStressLevel());
            dto.setRecommendation(entry.getAnalysis().getRecommendation());
        }
        return dto;
    }

    public MoodEntry toEntity(MoodEntryDto dto) {
        MoodEntry entry = new MoodEntry();
        entry.setMood(MoodType.valueOf(dto.getMoodType()));
        entry.setIntensity(dto.getIntensity());
        entry.setNote(dto.getNote());
        return entry;
    }
}
