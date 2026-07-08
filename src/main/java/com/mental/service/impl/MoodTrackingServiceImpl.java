package com.mental.service.impl;

import com.mental.dto.mood.MoodRequest;
import com.mental.mapper.MoodMapper;
import com.mental.dto.mood.MoodEntryDto;
import com.mental.model.entity.MoodAnalysis;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.repository.MoodTrackingRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import com.mental.service.MoodTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoodTrackingServiceImpl implements MoodTrackingService {


        private final MoodTrackingRepository moodTrackingRepository;
        private final MoodMapper moodMapper;
        private final UserRepository userRepository;
        /*
        @Override
        @Transactional
        public MoodEntryDto saveMood(MoodEntryDto mood, UserPrincipal principal) {
            if (principal == null) {
                throw new RuntimeException("Unauthorized: User session not found");
            }
            User user = userRepository.findByEmail(principal.getUsername())
                    .orElseGet(() -> userRepository.findByUsername(principal.getUsername())
                            .orElseThrow(() -> new RuntimeException("User not found: " + principal.getUsername())));

            MoodEntry entry = moodMapper.toEntity(mood);
            entry.setUser(user);

            MoodAnalysis analysis = new MoodAnalysis();
            analysis.setEmotion(mood.getEmotion());
            analysis.setStressLevel(mood.getStressLevel());
            analysis.setRecommendation(mood.getRecommendation());
            analysis.setMoodEntry(entry);
            entry.setAnalysis(analysis);

            MoodEntry savedEntry = moodTrackingRepository.save(entry);
            return moodMapper.toDto(savedEntry);
        }**/




    @Override
    public List<MoodEntry> findWeeklyByStatus(String name) {
        Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        return moodTrackingRepository.findWeeklyData(name, oneWeekAgo);

    }

    @Override
    public List<MoodEntry> findMonthlyStatus(String name) {
        Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        return moodTrackingRepository.findMonthlyData(name, oneMonthAgo);
        }

    @Override
    @Transactional
    public void deleteMood(Long id, String name) {
        MoodEntry mood = moodTrackingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mood entry not found"));
        if (!mood.getUser().getEmail().equals(name)) {
            throw new RuntimeException("Unauthorized access: You cannot delete this entry");
        }
        moodTrackingRepository.delete(mood);
    }

    @Override
    public void saveMood(String username, MoodRequest request) {
        // 1. Fetch the user who is currently logged in
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Map DTO to Entity
        MoodEntry entry = new MoodEntry();
        entry.setUser(user);
        entry.setMood(request.mood()); // Enum mapping
        entry.setIntensity(request.intensity());
        entry.setNote(request.note());
        entry.setCreatedAt(Instant.now());

        // 3. Save to database
        moodTrackingRepository.save(entry);

    }

    @Override
    public Map<String, Double> getMoodSummary(String username) {
        List<MoodEntry> moods = moodTrackingRepository.findByUserUsername(username);
        if (moods.isEmpty()) return Collections.emptyMap();

        long total = moods.size();

        return moods.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMood().name(), // Group by MoodType name (e.g., "HAPPY")
                        Collectors.collectingAndThen(
                                Collectors.counting(),
                                count -> (double) count / total * 100 // Calculate percentage
                        )
                ));    }
}
