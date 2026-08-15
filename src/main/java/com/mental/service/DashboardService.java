package com.mental.service;

import com.mental.dto.home.DashboardResponse;
import com.mental.dto.home.TodayMoodDto;
import com.mental.dto.home.WeeklyDayDto;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MoodType;
import com.mental.repository.MoodTrackingRepository;
import com.mental.repository.NotificationRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MoodTrackingRepository moodTrackingRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository; // Injected NotificationRepository

    public DashboardResponse getDashboardData(String email) {
        log.debug("Fetching dashboard data for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        LocalDate today = LocalDate.now();
        boolean hasUnread = notificationRepository.countByUserIdAndIsReadFalse(user.getId()) > 0;

        return DashboardResponse.builder()
                .greeting(buildGreeting(user.getUsername()))
                .formattedDate(today.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                .todayMood(getTodayMood(email, today))
                .weeklyOverview(getWeeklyOverview(email, today))
                .unreadNotification(hasUnread) // Set boolean flag
                .build();
    }

    private TodayMoodDto getTodayMood(String email, LocalDate today) {
        return moodTrackingRepository.findFirstByUserEmailAndDateOrderByCreatedAtDesc(email, today)
                .map(entry -> TodayMoodDto.builder()
                        .hasLogged(true)
                        .mood(entry.getMood())
                        .percentage(entry.getScore())
                        .message(getEncouragingMessage(entry.getMood()))
                        .build())
                .orElse(TodayMoodDto.builder()
                        .hasLogged(false)
                        .mood(null)
                        .percentage(0)
                        .message("How are you feeling today?")
                        .build());
    }

    private List<WeeklyDayDto> getWeeklyOverview(String email, LocalDate today) {
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        List<MoodEntry> entries = moodTrackingRepository
                .findByUserEmailAndDateBetweenOrderByDateAsc(email, startOfWeek, endOfWeek);

        Map<DayOfWeek, MoodEntry> latestEntryPerDay = new EnumMap<>(DayOfWeek.class);
        for (MoodEntry entry : entries) {
            DayOfWeek day = entry.getDate().getDayOfWeek();
            if (!latestEntryPerDay.containsKey(day) ||
                    entry.getCreatedAt().isAfter(latestEntryPerDay.get(day).getCreatedAt())) {
                latestEntryPerDay.put(day, entry);
            }
        }

        List<WeeklyDayDto> weeklyList = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            MoodEntry entry = latestEntryPerDay.get(day);
            boolean hasData = entry != null;

            Integer score = 0;
            if (hasData) {
                Integer existingScore = entry.getScore();

                if (existingScore != null && existingScore > 0) {
                    score = existingScore;
                } else if (entry.getMood() != null) {
                    int intensity = entry.getIntensity() != null ? entry.getIntensity() : 5;
                    score = calculateFallbackScore(entry.getMood(), intensity);
                }
            }

            weeklyList.add(WeeklyDayDto.builder()
                    .day(day)
                    .dayLabel(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .score(score)
                    .mood(hasData ? entry.getMood() : null)
                    .hasData(hasData)
                    .build());
        }

        return weeklyList;
    }

    private int calculateFallbackScore(MoodType mood, int intensity) {
        int baseScore = switch (mood) {
            case HAPPY -> 90;
            case CALM -> 85;
            case NEUTRAL -> 60;
            case SAD -> 40;
            case ANXIOUS -> 35;
            case ANGRY -> 20;
        };

        double intensityAdjustment = (intensity - 5) * 4.0;
        return (int) Math.max(0, Math.min(100, Math.round(baseScore + intensityAdjustment)));
    }

    private String buildGreeting(String userName) {
        int hour = LocalTime.now().getHour();
        String timeOfDay = (hour < 12) ? "Good morning" : (hour < 17) ? "Good afternoon" : "Good evening";
        return String.format("%s, %s", timeOfDay, userName != null ? userName : "User");
    }

    private String getEncouragingMessage(MoodType mood) {
        if (mood == null) return "Take a moment for yourself today.";
        return switch (mood) {
            case HAPPY -> "Great! Keep shining ✨";
            case CALM -> "Peace comes from within 🌿";
            case NEUTRAL -> "Steady and balanced ⚖️";
            case SAD -> "It's okay to feel sad. Be gentle with yourself 💙";
            case ANXIOUS -> "Take a deep breath. You're safe 🧘";
            case ANGRY -> "Pause and reset. Take time for you 🌊";
        };
    }
}