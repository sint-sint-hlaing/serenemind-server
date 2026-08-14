package com.mental.service.impl;

import com.mental.dto.mood.DailyMoodResponse;
import com.mental.dto.mood.MoodDistributionDto;
import com.mental.dto.mood.MoodRequest;
import com.mental.dto.mood.WeeklyMoodResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.MoodMapper;
import com.mental.model.entity.MoodEntry;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MoodType;
import com.mental.repository.MoodTrackingRepository;
import com.mental.repository.UserRepository;
import com.mental.service.MoodTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoodTrackingServiceImpl implements MoodTrackingService {

    private final MoodTrackingRepository moodTrackingRepository;
    private final MoodMapper moodMapper;
    private final UserRepository userRepository;

    // ==================== HELPER METHODS ====================

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    // ==================== SAVE ====================

    @Override
    @Transactional
    public void saveMood(String email, MoodRequest request) {
        log.info("Saving mood for user: {}", email);

        User user = findUserByEmail(email);
        LocalDate today = LocalDate.now();

        // Check if mood already saved for today
       /** boolean alreadySaved = moodTrackingRepository.existsByUserIdAndDate(
                user.getId(),
                today
        );

        if (alreadySaved) {
            throw new IllegalStateException("You have already saved a mood for today.");
        }*/

        // Calculate score based on mood percentage and intensity
        int score = calculateMoodScore(request.mood(), request.intensity());

        // Create entity with calculated score
        MoodEntry entry = moodMapper.toEntity(request, user);
        entry.setDate(today);
        entry.setScore(score); // Set calculated score

        moodTrackingRepository.save(entry);

        log.info("Mood saved successfully for user: {} with score: {}", email, score);
    }


    private int calculateMoodScore(MoodType mood, int intensity) {
        // Validate intensity
        if (intensity < 1 || intensity > 100) {
            log.warn("Invalid intensity: {}, defaulting to 5", intensity);
            intensity = Math.min(10, Math.max(1, intensity)); // Clamp to valid range
        }
        // Get base percentage from enum
        int baseScore = mood.getPercentage();

        // Intensity adjustment: -20% to +20%
        // Intensity 1 = -20%, 5 = 0%, 10 = +20%
        double intensityAdjustment = (intensity - 5) * 4.0; // Range: -20 to +20

        double finalScore = baseScore + intensityAdjustment;

        // Clamp between 0 and 100
        return (int) Math.max(0, Math.min(100, Math.round(finalScore)));
    }

    // ==================== SUMMARY ====================

    @Override
    public Map<String, Double> getMoodSummary(String email) {
        log.debug("Fetching mood summary for user: {}", email);

        List<MoodEntry> moods = moodTrackingRepository.findByEmail(email);
        if (moods.isEmpty()) {
            return Collections.emptyMap();
        }

        double total = moods.size();
        return moods.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMood().name(),
                        Collectors.collectingAndThen(Collectors.counting(), count -> (count / total) * 100)
                ));
    }

    // ==================== HISTORY ====================

    @Override
    public List<DailyMoodResponse> getMoodHistory(String email, int year, int month) {
        log.debug("Fetching mood history for user: {} - {}/{}", email, year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return moodTrackingRepository.findByEmailAndDateBetween(email, startDate, endDate)
                .stream()
                .map(moodMapper::toDailyResponse)
                .collect(Collectors.toList());
    }

    // ==================== BY DATE ====================

    @Override
    public DailyMoodResponse getMoodByDate(String email, LocalDate date) {
        log.debug("Fetching mood for user: {} on date: {}", email, date);

        // ✅ Method 1: Use findFirst (JPA Method)
        MoodEntry mood = moodTrackingRepository
                .findFirstByUserEmailAndDateOrderByCreatedAtDesc(email, date)
                .orElseThrow(() -> new ResourceNotFoundException("Mood not found for date: " + date));

        return moodMapper.toDailyResponse(mood);
    }

    // ==================== WEEKLY (Daily Entries) ====================

    @Override
    public List<WeeklyMoodResponse> getWeeklyMood(String email) {
        log.debug("Fetching weekly mood for user: {}", email);

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<MoodEntry> entries = moodTrackingRepository
                .findByUserEmailAndDateBetweenOrderByDateAsc(email, weekAgo, today);

        if (entries.isEmpty()) {
            return createEmptyWeeklyMood(weekAgo, today);
        }

        List<WeeklyMoodResponse> responses = new ArrayList<>();

        for (DayOfWeek day : DayOfWeek.values()) {
            // ထိုနေ့အတွက် မှတ်တမ်းရှိမရှိ စစ်ဆေးခြင်း
            Optional<MoodEntry> entryForDay = entries.stream()
                    .filter(entry -> entry.getDate().getDayOfWeek() == day)
                    .reduce((first, second) -> {
                        // တစ်နေ့တည်းတွင် မှတ်တမ်းများစွာရှိပါက နောက်ဆုံးတစ်ခုကို ယူပါ
                        return first.getCreatedAt().isAfter(second.getCreatedAt()) ? first : second;
                    });

            if (entryForDay.isPresent()) {
                // ✅ မှတ်တမ်းရှိသောနေ့
                MoodEntry entry = entryForDay.get();
                responses.add(WeeklyMoodResponse.createDailyEntry(
                        day,
                        entry.getMood(),
                        entry.getScore(),        // ✅ သိမ်းဆည်းထားသော Score ကို သုံးပါ
                        entry.getIntensity(),
                        entry.getNote()
                ));

                log.debug("Day: {}, Mood: {}, Score: {}, Intensity: {}",
                        day, entry.getMood(), entry.getScore(), entry.getIntensity());
            } else {
                // ✅ မှတ်တမ်းမရှိသောနေ့
                responses.add(WeeklyMoodResponse.createEmptyDailyEntry(day));
                log.debug("Day: {}, No entry found", day);
            }
        }

        return responses;
    }


    // ==================== MONTHLY ====================

    @Override
    public List<DailyMoodResponse> getMonthlyMood(String email) {
        log.debug("Fetching monthly mood for user: {}", email);

        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(30);

        return moodTrackingRepository
                .findByUserEmailAndDateBetweenOrderByDateAsc(email, monthAgo, today)
                .stream()
                .map(moodMapper::toDailyResponse)
                .collect(Collectors.toList());
    }

    // ==================== WEEKLY SUMMARY ====================

    @Override
    public WeeklyMoodResponse getWeeklySummary(String email) {
        log.debug("Fetching weekly summary for user: {}", email);

        List<WeeklyMoodResponse> weeklyMoods = getWeeklyMood(email);

        // ✅ Filter out days with no entries
        List<WeeklyMoodResponse> validMoods = weeklyMoods.stream()
                .filter(m -> m.percentage() != null && m.percentage() > 0)
                .collect(Collectors.toList());

        if (validMoods.isEmpty()) {
            log.debug("No weekly mood data found for user: {}", email);
            // ✅ Empty summary ကို ပြန်ပေးခြင်း
            return WeeklyMoodResponse.createSummary(
                    MoodType.NEUTRAL,
                    0,
                    0,
                    LocalDate.now().minusDays(7),
                    LocalDate.now()
            );
        }

        // ✅ Calculate average intensity
        double avgIntensity = validMoods.stream()
                .mapToInt(m -> m.intensity() != null ? m.intensity() : 0)
                .average()
                .orElse(0.0);

        // ✅ Find most common mood
        Map<MoodType, Long> moodCounts = validMoods.stream()
                .map(WeeklyMoodResponse::mood)
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()));

        MoodType dominantMood = moodCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(MoodType.NEUTRAL);

        // ✅ Create summary with all fields populated
        return WeeklyMoodResponse.createSummary(
                dominantMood,
                (int) Math.round(avgIntensity),
                validMoods.size(),
                LocalDate.now().minusDays(7),
                LocalDate.now()
        );
    }
    // ==================== DELETE ====================

    @Override
    @Transactional
    public void deleteMood(Long id, String email) {
        log.info("Deleting mood entry: {} for user: {}", id, email);

        MoodEntry mood = moodTrackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mood entry not found with id: " + id));

        if (!mood.getUser().getEmail().equals(email)) {
            throw new SecurityException("Unauthorized to delete this mood entry");
        }

        moodTrackingRepository.delete(mood);
        log.info("Mood entry deleted successfully: {}", id);
    }

    // ==================== DISTRIBUTION ====================

    @Override
    public List<MoodDistributionDto> getMoodDistribution() {
        log.debug("Fetching mood distribution");

        List<Object[]> distribution = moodTrackingRepository.getMoodDistribution();

        if (distribution.isEmpty()) {
            return new ArrayList<>();
        }

        long total = distribution.stream()
                .mapToLong(row -> (long) row[1])
                .sum();

        return distribution.stream()
                .map(row -> {
                    MoodType mood = (MoodType) row[0];
                    long count = (long) row[1];
                    double percentage = total > 0 ? (count * 100.0) / total : 0.0;
                    return new MoodDistributionDto(mood, count, percentage);
                })
                .collect(Collectors.toList());
    }

    // ==================== ANALYSIS ====================

    @Override
    public List<MoodDistributionDto> getMoodAnalysis(int days) {

        LocalDate startDate = LocalDate.now()
                .minusDays(days);


        List<Object[]> analysis =
                moodTrackingRepository.getMoodAnalysis(startDate);


        long total = analysis.stream()
                .mapToLong(row ->
                        ((Number) row[1]).longValue()
                )
                .sum();


        return analysis.stream()
                .map(row -> {

                    MoodType mood =
                            (MoodType) row[0];


                    long count =
                            ((Number) row[1]).longValue();


                    double percentage =
                            total > 0
                                    ? (count * 100.0) / total
                                    : 0;


                    return new MoodDistributionDto(
                            mood,
                            count,
                            percentage
                    );

                })
                .collect(Collectors.toList());
    }

    // ==================== DEPRECATED METHODS ====================


    @Override
    public List<MoodEntry> findWeeklyByStatus(String email) {
        log.warn("findWeeklyByStatus is deprecated. Use getWeeklyMood instead.");
        try {

            LocalDate weekAgo = LocalDate.now().minusDays(7);
            return moodTrackingRepository.findWeeklyData(email, weekAgo);
        } catch (Exception e) {
            log.error("Error in findWeeklyByStatus", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MoodEntry> findMonthlyStatus(String email) {
        log.warn("findMonthlyStatus is deprecated. Use getMonthlyMood instead.");
        try {
            LocalDate monthAgo = LocalDate.now().minusDays(30);
            return moodTrackingRepository.findMonthlyData(email, monthAgo);
        } catch (Exception e) {
            log.error("Error in findMonthlyStatus", e);
            return Collections.emptyList();
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private Integer getMoodPercentage(MoodType mood) {
        if (mood == null) {
            return 0;
        }
        return switch (mood) {
            case HAPPY -> 90;
            case CALM -> 85;
            case NEUTRAL -> 60;
            case SAD -> 40;
            case ANXIOUS -> 35;
            case ANGRY -> 20;
        };
    }

    private List<WeeklyMoodResponse> createEmptyWeeklyMood(LocalDate startDate, LocalDate endDate) {
        List<WeeklyMoodResponse> emptyResponses = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            emptyResponses.add(WeeklyMoodResponse.builder()
                    .day(currentDate.getDayOfWeek())
                    .mood(MoodType.NEUTRAL)
                    .percentage(0)
                    .intensity(0)
                    .build());
            currentDate = currentDate.plusDays(1);
        }

        return emptyResponses;
    }
}