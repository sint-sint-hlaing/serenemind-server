package com.mental.mapper;

import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodRequest;
import com.mental.dto.mood.WeeklyMoodResponse;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MoodType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

@Component
public class MoodMapper {

    public MoodEntry toEntity(MoodRequest request, User user) {
        MoodEntry entry = new MoodEntry();
        entry.setUser(user);
        entry.setMood(request.mood());
        entry.setIntensity(request.intensity());
        entry.setNote(request.note());
        // Score will be set separately in service
        return entry;
    }

    public DailyMoodResponse toDailyResponse(MoodEntry entry) {
        return DailyMoodResponse.builder()
                .date(entry.getDate())
                .mood(entry.getMood())
                .emoji(entry.getMood() != null ? entry.getMood().getEmoji() : "😐")
                .intensity(entry.getIntensity())
                .score(entry.getScore())
                .message(getMoodMessage(entry.getScore(), entry.getMood()))
                .note(entry.getNote())
                .build();
    }

    public WeeklyMoodResponse toWeeklyResponse(MoodEntry entry, DayOfWeek day) {
        return WeeklyMoodResponse.builder()
                .day(day != null ? day : entry.getDate().getDayOfWeek())
                .mood(entry.getMood())
                .percentage(entry.getScore())
                .intensity(entry.getIntensity())
                .note(entry.getNote())
                .build();
    }

    private String getMoodMessage(Integer score, MoodType mood) {
        if (score == null) {
            return "Mood recorded";
        }
        if (score >= 80) {
            return "You're feeling great today! Keep it up! 🌟";
        } else if (score >= 60) {
            return "You're doing well! 😊";
        } else if (score >= 40) {
            return "It's okay to not be okay. Take care of yourself. 💙";
        } else {
            return "You're going through a tough time. Be kind to yourself. 🌿";
        }
    }
}