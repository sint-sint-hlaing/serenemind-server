package com.mental.service;

import com.mental.dto.home.*;
import com.mental.dto.mood.WeeklyMoodResponse;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final MoodTrackingRepository moodRepository;
    private final NotificationRepository notificationRepository;

    public DashboardResponse getDashboardData(String email) {
        log.debug("Fetching dashboard data for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

        return DashboardResponse.builder()
                .username(user.getUsername())
                .greeting(getGreeting())
                .date(LocalDate.now())
                .todayMood(getTodayMood(user))
                .weeklyOverview(getWeeklyMood(user))
                .quickActions(getQuickActions())
                .unreadNotificationCount(unreadCount)
                .build();
    }

    private TodayMoodResponse getTodayMood(User user) {
        return moodRepository
                .findTopByUserAndDateOrderByCreatedAtDesc(user, LocalDate.now())
                .map(latest -> TodayMoodResponse.builder()
                        .mood(latest.getMood())
                        .percentage(latest.getMood().getPercentage())
                        .message(latest.getMood().getMessage())
                        .build())
                .orElse(null);
    }

    private List<WeeklyMoodResponse> getWeeklyMood(User user) {
        List<WeeklyMoodResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (DayOfWeek day : DayOfWeek.values()) {
            LocalDate date = monday.plusDays(day.getValue() - 1);

            if (date.isAfter(today)) {
                result.add(WeeklyMoodResponse.builder()
                        .day(day)
                        .percentage(0)
                        .mood(null)
                        .build());
                continue;
            }

            MoodEntry moodEntry = moodRepository
                    .findTopByUserAndDateOrderByCreatedAtDesc(user, date)
                    .orElse(null);

            result.add(WeeklyMoodResponse.builder()
                    .day(day)
                    .percentage(moodEntry != null ? moodEntry.getMood().getPercentage() : 0)
                    .mood(moodEntry != null ? moodEntry.getMood() : MoodType.NEUTRAL)
                    .build());
        }

        return result;
    }

    private String getGreeting() {
        int hour = LocalTime.now().getHour();

        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    private List<QuickActionResponse> getQuickActions() {
        return List.of(
                new QuickActionResponse("Journal", "journal"),
                new QuickActionResponse("Meditate", "meditation"),
                new QuickActionResponse("Goals", "goal"),
                new QuickActionResponse("SereneAI", "sereneAI")
        );
    }
}