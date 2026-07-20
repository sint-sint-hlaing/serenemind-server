package com.mental.mapper;

import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodRequest;
import com.mental.dto.mood.WeeklyMoodResponse;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class MoodMapper {

    public MoodEntry toEntity(MoodRequest request, User user) {
        if (request == null) {
            return null;
        }

        return MoodEntry.builder()
                .user(user)
                .mood(request.mood())
                .intensity(request.intensity())
                .score(request.score() != null ? request.score() : 0)
                .note(request.note())
                .date(LocalDate.now())
                .build();
        // createdAt နှင့် updatedAt ကို BaseEntity က @CreationTimestamp နှင့် @UpdateTimestamp ဖြင့် auto handle လုပ်ပေးမည်
    }

    public DailyMoodResponse toDailyResponse(MoodEntry entry) {
        if (entry == null) {
            return null;
        }

        return DailyMoodResponse.builder()
                .date(entry.getDate())
                .mood(entry.getMood())
                .intensity(entry.getIntensity())
                .score(entry.getScore())
                .note(entry.getNote())
                .build();
    }

    public WeeklyMoodResponse toWeeklyResponse(MoodEntry entry) {
        if (entry == null) {
            return null;
        }

        return WeeklyMoodResponse.builder()
                .mood(entry.getMood())
                .intensity(entry.getIntensity())
                .totalEntries(1)
                .startDate(entry.getDate())
                .endDate(entry.getDate())
                .build();
    }
}