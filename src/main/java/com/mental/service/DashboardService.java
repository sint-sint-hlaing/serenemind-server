package com.mental.service;

import com.mental.dto.home.*;
import com.mental.dto.mood.WeeklyMoodResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.DashboardMapper;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MoodType;
import com.mental.repository.MoodTrackingRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final MoodTrackingRepository moodRepository;
    private final DashboardMapper dashboardMapper;

    public DashboardResponse getDashboardData(String email) {
        log.debug("Fetching dashboard data for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return DashboardResponse.builder()
                .username(user.getUsername())
                .greeting(getGreeting())
                .date(LocalDate.now())
                .todayMood(getTodayMood(user))
                .weeklyOverview(getWeeklyMood(user))
                .quickActions(getQuickActions())
                .currentStreak(user.getCurrentStreak())
                .isNewBest(user.getCurrentStreak() >= user.getLongestStreak())
                .build();
    }

    private TodayMoodResponse getTodayMood(User user) {
        MoodEntry latest = moodRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElse(null);

        if (latest == null) {
            return TodayMoodResponse.builder()
                    .mood(MoodType.NEUTRAL)
                    .percentage(MoodType.NEUTRAL.getPercentage())
                    .message(MoodType.NEUTRAL.getMessage())
                    .build();
        }

        return TodayMoodResponse.builder()
                .mood(latest.getMood())
                .percentage(latest.getMood().getPercentage())
                .message(latest.getMood().getMessage())
                .build();
    }

    private List<WeeklyMoodResponse> getWeeklyMood(User user) {
        List<WeeklyMoodResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (DayOfWeek day : DayOfWeek.values()) {
            LocalDate date = today.with(day);

            // If the date is in the future, skip or set to 0
            if (date.isAfter(today)) {
                continue;
            }

            int percentage = moodRepository
                    .findTopByUserAndDateOrderByCreatedAtDesc(user, date)
                    .map(mood -> mood.getMood().getPercentage())
                    .orElse(0);

            result.add(WeeklyMoodResponse.builder()
                    .day(day)
                    .percentage(percentage)
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
                new QuickActionResponse("Journal", "📓", "journal"),
                new QuickActionResponse("Meditation", "🧘", "meditation"),
                new QuickActionResponse("Goals", "🎯", "goal"),
                new QuickActionResponse("Breathing", "🌬️", "breathing")
        );
    }
}