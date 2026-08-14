package com.mental.dto.mood;

import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DailyMoodResponse(
        LocalDate date,
        MoodType mood,
        String emoji,
        int intensity,
        Integer score,
        String message,
        String note
) {
    // ===== Static Factory Methods =====

    public static DailyMoodResponse of(LocalDate date, MoodType mood, int intensity, Integer score, String note) {
        return DailyMoodResponse.builder()
                .date(date)
                .mood(mood)
                .emoji(mood != null ? mood.getEmoji() : "😐")
                .intensity(intensity)
                .score(score)
                .message(getMoodMessage(score, mood))
                .note(note)
                .build();
    }

    private static String getMoodMessage(Integer score, MoodType mood) {
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