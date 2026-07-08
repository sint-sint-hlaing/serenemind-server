package com.mental.service;

import com.mental.dto.ActionItem;
import com.mental.dto.DashboardResponse;
import com.mental.dto.WeeklyData;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId; // Required import
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final UserRepository userRepository;

    public DashboardResponse getDashboardData(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<MoodEntry> moodEntries = user.getMoods();
        MoodEntry latestMood = moodEntries.isEmpty() ? null : moodEntries.get(moodEntries.size() - 1);

        // Fixed mapping logic
        List<WeeklyData> weeklyDataList = moodEntries.stream()
                .map(m -> new WeeklyData(
                        m.getCreatedAt().atZone(ZoneId.systemDefault()).getDayOfWeek().name(),
                        (float) m.getIntensity()
                ))
                .collect(Collectors.toList());

        return new DashboardResponse(
                user.getUsername(),
                latestMood != null ? latestMood.getMood().name() : "None",
                latestMood != null ? latestMood.getIntensity() * 10 : 0,
                weeklyDataList,
                List.of(new ActionItem("Journal", "icon_url"), new ActionItem("Meditate", "icon_url"))
        );
    }
}