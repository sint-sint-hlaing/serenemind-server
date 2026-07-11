package com.mental.service.impl;

import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodRequest;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.MoodMapper;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.repository.MoodTrackingRepository;
import com.mental.repository.UserRepository;
import com.mental.service.MoodTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Default to read-only for safety and performance
public class MoodTrackingServiceImpl implements MoodTrackingService {

    private final MoodTrackingRepository moodTrackingRepository;
    private final MoodMapper moodMapper;
    private final UserRepository userRepository;

    // Helper method to fetch user, reducing code duplication
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public List<MoodEntry> findWeeklyByStatus(String email) {
        return moodTrackingRepository.findWeeklyData(email, Instant.now().minus(7, ChronoUnit.DAYS));
    }

    @Override
    public List<MoodEntry> findMonthlyStatus(String email) {
        return moodTrackingRepository.findMonthlyData(email, Instant.now().minus(30, ChronoUnit.DAYS));
    }

    @Override
    @Transactional
    public void deleteMood(Long id, String email) {
        MoodEntry mood = moodTrackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mood entry not found"));

        if (!mood.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Unauthorized access: You cannot delete this entry");
        }
        moodTrackingRepository.delete(mood);
    }

    @Override
    @Transactional
    public void saveMood(String email, MoodRequest request) {
        User user = findUserByEmail(email);

        // Standard constructor approach
        MoodEntry entry = new MoodEntry();
        entry.setUser(user);
        entry.setMood(request.mood());
        entry.setIntensity(request.intensity());
        entry.setNote(request.note());
        entry.setCreatedAt(Instant.now());

        moodTrackingRepository.save(entry);
    }

    @Override
    public Map<String, Double> getMoodSummary(String email) {
        List<MoodEntry> moods = moodTrackingRepository.findByEmail(email);
        if (moods.isEmpty()) return Collections.emptyMap();

        double total = moods.size();
        return moods.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMood().name(),
                        Collectors.collectingAndThen(Collectors.counting(), count -> (count / total) * 100)
                ));
    }

    @Override
    public List<DailyMoodResponse> getMoodHistory(String email, int year, int month) {
        return moodTrackingRepository.findByYearAndMonth(email, year, month)
                .stream()
                .map(entry -> new DailyMoodResponse(
                        entry.getCreatedAt().toString(),
                        entry.getMood().name(),
                        entry.getIntensity(),
                        entry.getNote()))
                .collect(Collectors.toList());
    }
}