package com.mental.dto.mood;

import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Builder
public record WeeklyMoodResponse(
        DayOfWeek day,
        MoodType mood,
        Integer percentage,
        Integer intensity,
        String note,
        Integer totalEntries,
        LocalDate startDate,
        LocalDate endDate
) {
    // ===== Validation =====
    public WeeklyMoodResponse {
        if (percentage != null && (percentage < 0 || percentage > 100)) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        if (intensity != null && (intensity < 0 || intensity > 10)) {
            throw new IllegalArgumentException("Intensity must be between 0 and 10");
        }
        if (totalEntries != null && totalEntries < 0) {
            throw new IllegalArgumentException("Total entries cannot be negative");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    // ===== Static Factory Methods =====

    public static WeeklyMoodResponse of(DayOfWeek day, MoodType mood, Integer percentage) {
        return WeeklyMoodResponse.builder()
                .day(day)
                .mood(mood)
                .percentage(percentage)
                .build();
    }

    public static WeeklyMoodResponse of(DayOfWeek day, MoodType mood, Integer percentage, Integer intensity) {
        return WeeklyMoodResponse.builder()
                .day(day)
                .mood(mood)
                .percentage(percentage)
                .intensity(intensity)
                .build();
    }

    public static WeeklyMoodResponse of(DayOfWeek day, MoodType mood, Integer percentage, Integer intensity, String note) {
        return WeeklyMoodResponse.builder()
                .day(day)
                .mood(mood)
                .percentage(percentage)
                .intensity(intensity)
                .note(note)
                .build();
    }

    public static WeeklyMoodResponse summary(MoodType dominantMood, Integer intensity,
                                             Integer totalEntries, LocalDate startDate, LocalDate endDate) {
        return WeeklyMoodResponse.builder()
                .mood(dominantMood)
                .intensity(intensity)
                .totalEntries(totalEntries)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    public static WeeklyMoodResponse emptySummary() {
        return WeeklyMoodResponse.builder()
                .mood(MoodType.NEUTRAL)
                .intensity(0)
                .totalEntries(0)
                .startDate(LocalDate.now().minusDays(7))
                .endDate(LocalDate.now())
                .build();
    }

    // ===== Helper Methods =====

    public boolean isDailyEntry() {
        return day != null;
    }

    public boolean isSummary() {
        return startDate != null && endDate != null;
    }

    public String getDateRange() {
        if (startDate != null && endDate != null) {
            return startDate + " ~ " + endDate;
        }
        return null;
    }

    public String getDayName() {
        return day != null ? day.name() : null;
    }

    public String getMoodEmoji() {
        return mood != null ? mood.getEmoji() : "😐";
    }

    public String getMoodMessage() {
        return mood != null ? mood.getMessage() : "No mood recorded";
    }
}