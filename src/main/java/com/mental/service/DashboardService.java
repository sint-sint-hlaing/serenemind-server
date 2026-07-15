package com.mental.service;


import com.mental.dto.home.*;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MoodType;
import com.mental.repository.MoodTrackingRepository;
import com.mental.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.*;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DashboardService {


    private final UserRepository userRepository;
    private final MoodTrackingRepository moodRepository;



    public DashboardResponse getDashboardData(String email){


        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException("User not found")
                );


        return DashboardResponse.builder()

                .username(user.getUsername())

                .greeting(getGreeting())

                .date(LocalDate.now())

                .todayMood(getTodayMood(user))

                .weeklyOverview(getWeeklyMood(user))

                .quickActions(getQuickActions())

                .build();

    }




    private TodayMoodResponse getTodayMood(User user){


        MoodEntry latest =
                moodRepository
                        .findTopByUserOrderByCreatedAtDesc(user)
                        .orElse(null);



        if(latest == null){

            return TodayMoodResponse.builder()

                    .mood(MoodType.NEUTRAL)

                    .percentage(50)

                    .message("How are you feeling today? 🌱")

                    .build();

        }



        MoodType mood = latest.getMood();



        return TodayMoodResponse.builder()

                .mood(mood)

                .percentage(getMoodPercentage(mood))

                .message(getMoodMessage(mood))

                .build();

    }





    private List<WeeklyMoodResponse> getWeeklyMood(User user){


        List<WeeklyMoodResponse> result =
                new ArrayList<>();



        for(DayOfWeek day: DayOfWeek.values()){


            result.add(

                    WeeklyMoodResponse.builder()

                            .day(day)

                            .mood(MoodType.NEUTRAL)

                            .percentage(50)

                            .build()

            );


        }


        return result;

    }





    private Integer getMoodPercentage(MoodType mood){


        return switch(mood){

            case HAPPY -> 90;

            case CALM -> 85;

            case NEUTRAL -> 60;

            case SAD -> 40;

            case ANXIOUS -> 35;

            case ANGRY -> 20;

        };

    }




    private String getMoodMessage(MoodType mood){


        return switch(mood){

            case HAPPY ->
                    "Keep smiling today 😊";

            case CALM ->
                    "Stay peaceful and relaxed 😌";

            case NEUTRAL ->
                    "Today is a fresh start 🌱";

            case SAD ->
                    "Take care of yourself 💙";

            case ANXIOUS ->
                    "Take a deep breath 🌿";

            case ANGRY ->
                    "Relax and stay calm ❤️";

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





    private List<QuickActionResponse> getQuickActions(){


        return List.of(

                new QuickActionResponse(
                        "Journal",
                        "📓",
                        "journal"
                ),

                new QuickActionResponse(
                        "Meditation",
                        "🧘",
                        "meditation"
                ),

                new QuickActionResponse(
                        "Goals",
                        "🎯",
                        "goal"
                ),

                new QuickActionResponse(
                        "Breathing",
                        "🌬️",
                        "breathing"
                )

        );

    }

}