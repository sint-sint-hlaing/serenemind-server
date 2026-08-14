package com.mental.dto.mood;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mental.model.entity.enums.MoodType;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

public record WeeklyMoodResponse(
        // ===== Daily Entry Fields =====
        DayOfWeek day,
        MoodType mood,
        Integer percentage,
        Integer intensity,
        String note,

        // ===== Summary Fields =====
        Integer totalEntries,
        LocalDate startDate,
        LocalDate endDate,

        // ===== Computed Fields =====
        boolean dailyEntry,
        String dayName,
        String moodEmoji,
        String moodMessage,
        boolean summary
) {
    // ===== Factory Methods =====

    /**
     * Daily Entry အတွက် Factory Method
     */
    public static WeeklyMoodResponse createDailyEntry(DayOfWeek day, MoodType mood,
                                                      Integer percentage, Integer intensity,
                                                      String note) {
        return WeeklyMoodResponse.builder()
                .day(day)
                .mood(mood != null ? mood : MoodType.NEUTRAL)
                .percentage(percentage != null ? percentage : 0)
                .intensity(intensity != null ? intensity : 0)
                .note(note)
                .totalEntries(null)
                .startDate(null)
                .endDate(null)
                .dailyEntry(true)
                .dayName(day != null ? day.name() : null)
                .moodEmoji(mood != null ? mood.getEmoji() : "😐")
                .moodMessage(mood != null ? mood.getMessage() : "No mood recorded")
                .summary(false)
                .build();
    }

    /**
     * Empty Daily Entry အတွက် Factory Method
     */
    public static WeeklyMoodResponse createEmptyDailyEntry(DayOfWeek day) {
        return WeeklyMoodResponse.builder()
                .day(day)
                .mood(MoodType.NEUTRAL)
                .percentage(0)
                .intensity(0)
                .note(null)
                .totalEntries(null)
                .startDate(null)
                .endDate(null)
                .dailyEntry(true)
                .dayName(day != null ? day.name() : null)
                .moodEmoji("🌱")
                .moodMessage("Today is a fresh start")
                .summary(false)
                .build();
    }

    /**
     * ✅ Weekly Summary အတွက် Factory Method (ပြင်ဆင်ထားသည်)
     */
    public static WeeklyMoodResponse createSummary(MoodType dominantMood,
                                                   Integer intensity,
                                                   Integer totalEntries,
                                                   LocalDate startDate,
                                                   LocalDate endDate) {
        // ✅ dateRange ကို သေချာပြင်ဆင်ခြင်း
        String dateRange = null;
        if (startDate != null && endDate != null) {
            dateRange = startDate + " ~ " + endDate;
        } else if (startDate != null) {
            dateRange = startDate + " ~ present";
        } else if (endDate != null) {
            dateRange = "past ~ " + endDate;
        }

        // ✅ dominantMood မရှိပါက NEUTRAL ထားရန်
        MoodType finalMood = dominantMood != null ? dominantMood : MoodType.NEUTRAL;

        return WeeklyMoodResponse.builder()
                .day(null)
                .mood(finalMood)
                .percentage(null)
                .intensity(intensity != null ? intensity : 0)
                .note(null)
                .totalEntries(totalEntries != null ? totalEntries : 0)
                .startDate(startDate)
                .endDate(endDate)
                .dailyEntry(false)
                .dayName(null)
                .moodEmoji(finalMood.getEmoji())  // ✅ Emoji ထည့်ပေးခြင်း
                .moodMessage(finalMood.getMessage())  // ✅ Message ထည့်ပေးခြင်း
                .summary(true)  // ✅ summary ကို true ထားရန်
                .build();
    }
}