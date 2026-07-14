package com.mental.service;

import com.mental.dto.emotional.EmotionResponse;
import com.mental.dto.home.DashboardResponse;
import com.mental.dto.home.QuickActionResponse;
import com.mental.dto.home.TodayMoodResponse;
import com.mental.dto.home.WeeklyMoodResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MoodType;
import com.mental.repository.MoodTrackingRepository;
import com.mental.repository.UserRepository;
import com.mental.service.python.EmotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final UserRepository userRepository;
    private final MoodTrackingRepository moodRepository;
    private final EmotionService emotionService;

    public DashboardResponse getDashboardData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<WeeklyMoodResponse> weekly = getWeeklyMood(user);

        return DashboardResponse.builder()
                .username(user.getUsername())
                .todayMood(getTodayMood(user))
                .greeting(getGreeting())
                .date(LocalDate.now())
                .weeklyOverview(weekly)
                .quickActions(getQuickActions())
                .build();

    }

    private TodayMoodResponse getTodayMood(User user) {

        MoodEntry latest = moodRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException("No mood found"));


        EmotionResponse ai =
                emotionService.analyze(latest.getNote());

        return TodayMoodResponse.builder()
                .mood(MoodType.valueOf(ai.getEmotion()))
                .percentage(ai.getPercentage())
                .message(ai.getMessage())
                .build();
       /* MoodType mood = latest.getMood();

        return TodayMoodResponse.builder()
                .mood(mood)
                .percentage(getMoodPercentage(mood)) // အပေါ်က သတ်မှတ်ထားတဲ့ manual percentage
                .message(getMoodMessage(mood))       // အပေါ်က သတ်မှတ်ထားတဲ့ manual message
                .build();*/

    }

    private Integer getMoodPercentage(MoodType mood) {

        return switch (mood) {
            case HAPPY -> 90;
            case CALM -> 85;
            case NEUTRAL -> 60;
            case SAD -> 40;
            case ANXIOUS -> 35;
            case ANGRY -> 20;
        };

    }

    private String getMoodMessage(MoodType mood) {

        return switch (mood) {

            case HAPPY -> "Keep smiling today 😊";

            case CALM -> "Stay peaceful and relaxed 😌";

            case NEUTRAL -> "Today is a fresh start 🌱";

            case SAD -> "Take care of yourself 💙";

            case ANXIOUS -> "Take a deep breath 🌿";

            case ANGRY -> "Relax and stay calm ❤️";
        };

    }

    private String getGreeting(){

        int hour = LocalTime.now().getHour();

        if(hour < 12)
            return "Good Morning";

        if(hour < 17)
            return "Good Afternoon";

        return "Good Evening";

    }
    private List<WeeklyMoodResponse> getWeeklyMood(User user){

        List<WeeklyMoodResponse> list = new ArrayList<>();

        for(DayOfWeek day : DayOfWeek.values()){

            list.add(

                    new WeeklyMoodResponse(

                            day,

                            MoodType.HAPPY,

                            ThreadLocalRandom.current()
                                    .nextInt(50,100)

                    )

            );

        }

        return list;

    }
    private List<QuickActionResponse> getQuickActions(){

        return List.of(

                new QuickActionResponse(
                        "Journal",
                        "journal",
                        "📓"
                ),

                new QuickActionResponse(
                        "Meditation",
                        "meditation",
                        "🧘"
                ),

                new QuickActionResponse(
                        "Goals",
                        "goal",
                        "🎯"
                ),

                new QuickActionResponse(
                        "Breathing",
                        "breathing",
                        "🌬️"
                )

        );

    }

}