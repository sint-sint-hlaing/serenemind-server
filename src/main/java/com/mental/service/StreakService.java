package com.mental.service;

import com.mental.dto.Streak.StreakResponse;
import com.mental.model.entity.User;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserRepository userRepository;

    @Transactional
    public StreakResponse updateStreak(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActiveDate();
        boolean isNewBest = false;

        if (lastActive == null) {
            user.setCurrentStreak(1);
            user.setLongestStreak(Math.max(user.getLongestStreak(), 1));
            user.setLastActiveDate(today);
        } else if (lastActive.isBefore(today)) {
            if (lastActive.equals(today.minusDays(1))) {
                int newStreak = user.getCurrentStreak() + 1;
                user.setCurrentStreak(newStreak);

                if (newStreak > user.getLongestStreak()) {
                    user.setLongestStreak(newStreak);
                    isNewBest = true;
                }
            } else {
                user.setCurrentStreak(1);
            }
            user.setLastActiveDate(today);
        }


        User savedUser = userRepository.save(user);
        return convertToStreakResponse(savedUser, isNewBest);
    }

    @Transactional(readOnly = true)
    public StreakResponse getStreakDetails(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        LocalDate today = LocalDate.now();
        if (user.getLastActiveDate() != null && user.getLastActiveDate().isBefore(today.minusDays(1))) {
            user.setCurrentStreak(0);
        }

        return convertToStreakResponse(user, false);
    }

    @Transactional
    public StreakResponse useStreakFreeze(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getStreakFreezeCount() <= 0) {
            throw new IllegalStateException("No streak freeze available");
        }

        user.setStreakFreezeCount(user.getStreakFreezeCount() - 1);
        user.setLastActiveDate(LocalDate.now().minusDays(1));


        User savedUser = userRepository.save(user);
        return updateStreak(email);
    }

    private StreakResponse convertToStreakResponse(User user, boolean isNewBest) {
        List<Boolean> weeklyOverview = new ArrayList<>();
        LocalDate today = LocalDate.now();


        LocalDate monday = today.with(DayOfWeek.MONDAY);


        for (int i = 0; i < 7; i++) {
            LocalDate currentDay = monday.plusDays(i);

         if (user.getLastActiveDate() != null && !currentDay.isAfter(today)) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(currentDay, user.getLastActiveDate());
                weeklyOverview.add(daysBetween >= 0 && daysBetween < user.getCurrentStreak());
            } else {
                weeklyOverview.add(false);
            }
        }

        return StreakResponse.builder()
                .currentStreak(user.getCurrentStreak())
                .longestStreak(user.getLongestStreak())
                .streakFreezeCount(user.getStreakFreezeCount())
                .isNewBest(isNewBest)
                .weeklyOverview(weeklyOverview)
                .build();
    }
}
