// MeditationService.java - Complete Fixed Version
package com.mental.service;

import com.mental.dto.meditation.MeditationSessionRequest;
import com.mental.dto.meditation.*;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.MeditationMapper;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MeditationCategory;
import com.mental.model.entity.enums.MeditationStatus;
import com.mental.model.entity.enums.MeditationTime;
import com.mental.repository.FavoriteRepository;
import com.mental.repository.MeditationRepository;
import com.mental.repository.MeditationSessionRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MeditationService {

    private final MeditationRepository meditationRepository;
    private final MeditationSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final MeditationMapper meditationMapper;
    private final FavoriteRepository favoriteRepository;

    // ===== CATEGORY MAP with emojis =====
    private static final Map<MeditationCategory, MeditationCategoryResponse> CATEGORY_MAP = new HashMap<>();
    static {
        CATEGORY_MAP.put(MeditationCategory.RELAXATION,
                new MeditationCategoryResponse("RELAXATION", "Relaxation", "🧘", "Find peace and calm"));
        CATEGORY_MAP.put(MeditationCategory.SLEEP,
                new MeditationCategoryResponse("SLEEP", "Sleep", "💤", "Drift into deep sleep"));
        CATEGORY_MAP.put(MeditationCategory.ANXIETY,
                new MeditationCategoryResponse("ANXIETY", "Anxiety Relief", "😌", "Calm your mind"));
        CATEGORY_MAP.put(MeditationCategory.FOCUS,
                new MeditationCategoryResponse("FOCUS", "Focus", "🎯", "Sharpen your concentration"));
        CATEGORY_MAP.put(MeditationCategory.BREATHING,
                new MeditationCategoryResponse("BREATHING", "Breathing", "🌬️", "Master your breath"));
        CATEGORY_MAP.put(MeditationCategory.STRESS,
                new MeditationCategoryResponse("STRESS", "Stress Relief", "🌿", "Release tension"));
    }

    // ===== TIME MAP with emojis =====
    private static final Map<MeditationTime, MeditationTimeResponse> TIME_MAP = new HashMap<>();
    static {
        TIME_MAP.put(MeditationTime.MORNING,
                new MeditationTimeResponse("MORNING", "Morning", "🌅", "Start your day right"));
        TIME_MAP.put(MeditationTime.AFTERNOON,
                new MeditationTimeResponse("AFTERNOON", "Afternoon", "☀️", "Midday refresh"));
        TIME_MAP.put(MeditationTime.EVENING,
                new MeditationTimeResponse("EVENING", "Evening", "🌆", "Wind down"));
        TIME_MAP.put(MeditationTime.NIGHT,
                new MeditationTimeResponse("NIGHT", "Night", "🌙", "Peaceful sleep"));
    }

    // ===== GET ALL MEDITATIONS =====
    @Transactional(readOnly = true)
    public List<MeditationResponse> getAll(MeditationCategory category) {
        List<Meditation> meditations = (category != null)
                ? meditationRepository.findByCategory(category)
                : meditationRepository.findAll();

        log.debug(category != null ? "Fetching meditations by category: {}" : "Fetching all meditations", category);

        return meditations.stream()
                .map(meditationMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== GET BY ID =====
    @Transactional(readOnly = true)
    public MeditationResponse getById(Long id, Long userId) {
        log.debug("Fetching meditation by id: {}", id);

        Meditation meditation = meditationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meditation not found with id: " + id));

        // Increment view count
        meditation.setViewCount(meditation.getViewCount() + 1);
        meditationRepository.save(meditation);

        MeditationResponse response = meditationMapper.toResponse(meditation);

        // Check if user has favorited
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                boolean isFavorite = favoriteRepository.existsByUserAndMeditation(user, meditation);
                response.setFavorite(isFavorite);
            }
        }

        return response;
    }

    // ===== DASHBOARD =====
    @Transactional(readOnly = true)
    public MeditationDashboardResponse getDashboard() {
        log.debug("Fetching meditation dashboard");
        Pageable limit = PageRequest.of(0, 10);

        // Get featured meditations
        List<MeditationResponse> featured = meditationRepository
                .findByStatusAndFeaturedTrueOrderByListenCountDesc(MeditationStatus.PUBLISHED)
                .stream()
                .limit(5)
                .map(meditationMapper::toResponse)
                .collect(Collectors.toList());

        // Get popular meditations
        List<MeditationResponse> popular = meditationRepository
                .findPopularMeditations(limit)
                .stream()
                .map(meditationMapper::toResponse)
                .collect(Collectors.toList());

        // Get recent meditations
        List<MeditationResponse> recent = meditationRepository
                .findRecentMeditations(limit)
                .stream()
                .map(meditationMapper::toResponse)
                .collect(Collectors.toList());

        // Get statistics
        MeditationStatistics statistics = getStatistics();

        return MeditationDashboardResponse.builder()
                .categories(CATEGORY_MAP.values().stream().collect(Collectors.toList()))
                .times(TIME_MAP.values().stream().collect(Collectors.toList()))
                .featured(featured)
                .popular(popular)
                .recent(recent)
                .statistics(statistics)
                .build();
    }

    // ===== STATISTICS =====
    @Transactional(readOnly = true)
    public MeditationStatistics getStatistics() {
        log.debug("Fetching meditation statistics");

        long totalMeditations = meditationRepository.countByStatus(MeditationStatus.PUBLISHED);
        long totalSessions = sessionRepository.count();
        Long totalMinutes = sessionRepository.sumDurationMinutes();

        if (totalMinutes == null) {
            totalMinutes = 0L;
        }

        return MeditationStatistics.builder()
                .totalMeditations(totalMeditations)
                .totalSessions(totalSessions)
                .totalMinutes(totalMinutes)
                .currentStreak(0L)
                .longestStreak(0L)
                .build();
    }

    // ===== COMPLETE SESSION =====
    @Transactional
    public void completeSession(String email, MeditationSessionRequest request) {
        log.info("Completing meditation session for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Meditation meditation = meditationRepository.findById(request.getMeditationId())
                .orElseThrow(() -> new ResourceNotFoundException("Meditation not found with id: " + request.getMeditationId()));

        // Update listen count
        meditation.setListenCount(meditation.getListenCount() + 1);
        meditationRepository.save(meditation);

        // Create or update session
        MeditationSession session = sessionRepository
                .findByUserAndMeditationId(user, meditation.getId())
                .orElse(MeditationSession.builder()
                        .user(user)
                        .meditation(meditation)
                        .build());
        Integer progressPercentage = calculateProgress(
                request.getDurationMinutes(),
                meditation.getTotalDurationMinutes(), // You need this field
                request.isCompleted()
        );


        session.setCompleted(request.isCompleted());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setProgressPercentage(progressPercentage); // Set calculated value
        session.setCompletedAt(LocalDateTime.now());

        sessionRepository.save(session);

        log.info("Meditation session completed for user: {}, meditation: {}", email, meditation.getTitle());
    }
    @Transactional(readOnly = true)
    public List<MeditationResponse> search(String query, MeditationCategory category, MeditationTime time) {
        log.debug("Searching meditations. query={}, category={}, time={}", query, category, time);

        // ✅ FIXED: Use the combined search method with all three parameters
        // Clean the query
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;

        // Use the combined search method (this handles all cases)
        return meditationRepository.search(cleanQuery, category, time)
                .stream()
                .map(meditationMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== GET HISTORY =====
    @Transactional(readOnly = true)
    public List<MeditationHistoryResponse> getHistory(String email) {
        log.debug("Fetching meditation history for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return sessionRepository.findByUserAndCompletedTrueOrderByCompletedAtDesc(user)
                .stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    // ===== GET RECOMMENDATIONS =====
    @Transactional(readOnly = true)
    public List<MeditationResponse> getRecommendations(Long userId) {
        log.debug("Fetching recommendations for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get user's completed meditations categories
        List<MeditationCategory> userCategories = sessionRepository
                .findByUserAndCompletedTrue(user)
                .stream()
                .map(session -> session.getMeditation().getCategory())
                .distinct()
                .collect(Collectors.toList());

        // If user has completed meditations, recommend from same categories
        if (!userCategories.isEmpty()) {
            return meditationRepository
                    .findByCategoryAndStatus(userCategories.get(0), MeditationStatus.PUBLISHED)
                    .stream()
                    .limit(10)
                    .map(meditationMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // Fallback: return popular meditations
        return meditationRepository
                .findPopularMeditations(PageRequest.of(0, 10))
                .stream()
                .map(meditationMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== CONTINUE LISTENING =====
    @Transactional(readOnly = true)
    public List<MeditationResponse> getContinueListening(Long userId) {
        log.debug("Fetching continue listening for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return sessionRepository
                .findIncompleteSessionsByUser(user)
                .stream()
                .map(session -> meditationMapper.toResponse(session.getMeditation()))
                .collect(Collectors.toList());
    }



    // ===== GET BY CATEGORY =====
    @Transactional(readOnly = true)
    public List<MeditationResponse> getByCategory(MeditationCategory category) {
        log.debug("Fetching meditations by category: {}", category);

        return meditationRepository.findByCategory(category)
                .stream()
                .map(meditationMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== GET BY TIME =====
    @Transactional(readOnly = true)
    public List<MeditationResponse> getByTime(MeditationTime time) {
        log.debug("Fetching meditations by time: {}", time);

        return meditationRepository.findByTimeOfDay(time)
                .stream()
                .map(meditationMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== TOGGLE FAVORITE =====
    @Transactional
    public FavoriteResponse toggleFavorite(Long userId, Long meditationId) {
        log.info("Toggling favorite for user: {}, meditation: {}", userId, meditationId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Meditation meditation = meditationRepository.findById(meditationId)
                .orElseThrow(() -> new ResourceNotFoundException("Meditation not found"));

        boolean isFavorite = favoriteRepository.existsByUserAndMeditation(user, meditation);

        if (isFavorite) {
            favoriteRepository.deleteByUserAndMeditation(user, meditation);
            meditation.setFavoriteCount(Math.max(0, meditation.getFavoriteCount() - 1));
            meditationRepository.save(meditation);
            return new FavoriteResponse(false, "Removed from favorites");
        } else {
            com.mental.model.entity.Favorite favorite = com.mental.model.entity.Favorite.builder()
                    .user(user)
                    .meditation(meditation)
                    .build();
            favoriteRepository.save(favorite);
            meditation.setFavoriteCount(meditation.getFavoriteCount() + 1);
            meditationRepository.save(meditation);
            return new FavoriteResponse(true, "Added to favorites");
        }
    }

    // ===== SAVE TIMER =====
    @Transactional
    public TimerResponse saveTimer(
            Long userId,
            Long meditationId,
            TimerRequest request) {

        log.info(
                "Saving timer: user={}, meditation={}, minutes={}",
                userId,
                meditationId,
                request.getMinutes()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Meditation meditation = meditationRepository.findById(meditationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Meditation not found"));

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime endsAt =
                now.plusMinutes(request.getMinutes());

        MeditationSession session =
                sessionRepository
                        .findByUserAndMeditationId(user, meditationId)
                        .orElse(
                                MeditationSession.builder()
                                        .user(user)
                                        .meditation(meditation)
                                        .build()
                        );

        session.setDurationMinutes(request.getMinutes());
        session.setStartedAt(now);
        session.setEndsAt(endsAt);

        session.setCompleted(false);
        session.setProgressPercentage(0);
        session.setCompletedAt(null);

        sessionRepository.save(session);

        return new TimerResponse(
                request.getMinutes(),
                "Timer set for "
                        + request.getMinutes()
                        + " minutes"
        );
    }

    // ===== GET SHARE LINK =====
    @Transactional(readOnly = true)
    public ShareResponse getShareLink(Long id) {
        log.debug("Getting share link for meditation: {}", id);

        Meditation meditation = meditationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meditation not found"));

        return ShareResponse.builder()
                .title(meditation.getTitle())
                .description(meditation.getDescription())
                .imageUrl(meditation.getImageUrl())
                .shareUrl("https://serenemind.com/meditations/" + meditation.getId())
                .build();
    }

    // ===== GET PREVIOUS =====
    @Transactional(readOnly = true)
    public MeditationResponse getPrevious(Long id) {
        log.debug("Getting previous meditation for id: {}", id);

        Meditation meditation = meditationRepository
                .findFirstByIdLessThanOrderByIdDesc(id)
                .orElseThrow(() -> new ResourceNotFoundException("Previous meditation not found"));

        return meditationMapper.toResponse(meditation);
    }

    // ===== GET NEXT =====
    @Transactional(readOnly = true)
    public MeditationList getNext(Long id) {
        log.debug("Getting next meditation for id: {}", id);

        Meditation meditation = meditationRepository
                .findFirstByIdGreaterThanOrderByIdAsc(id)
                .orElseThrow(() -> new ResourceNotFoundException("Next meditation not found"));

        return meditationMapper.toListResponse(meditation);
    }


    // ===== DOWNLOAD AUDIO =====
    @Transactional(readOnly = true)
    public Resource downloadAudio(Long id) {

        Meditation meditation = meditationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Meditation not found"));

        String audioUrl = meditation.getAudioUrl();

        if (audioUrl == null || audioUrl.isBlank()) {
            throw new ResourceNotFoundException("Audio URL not found");
        }

        try {
            return new UrlResource(audioUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid audio URL", e);
        }
    }

    // ===== MAPPERS =====
    private MeditationHistoryResponse mapToHistoryResponse(MeditationSession session) {
        return MeditationHistoryResponse.builder()
                .id(session.getId())
                .title(session.getMeditation().getTitle())
                .category(session.getMeditation().getCategory() != null ?
                        session.getMeditation().getCategory().name() : null)
                .duration(session.getDurationMinutes())
                .completed(session.isCompleted())
                .progressPercentage(session.getProgressPercentage())
                .completedAt(session.getCompletedAt())
                .imageUrl(session.getMeditation().getImageUrl())
                .build();
    }
    private Integer calculateProgress(Integer completedMinutes, Integer totalMinutes, boolean isCompleted) {
        // If marked as completed, it's 100%
        if (isCompleted) {
            return 100;
        }

        // If no duration data, return 0
        if (completedMinutes == null || completedMinutes <= 0) {
            return 0;
        }

        // If meditation has no total duration defined, treat any duration as progress
        if (totalMinutes == null || totalMinutes <= 0) {
            return Math.min(100, completedMinutes * 5); // e.g., 5% per minute
        }

        // Calculate percentage based on time
        int percentage = (completedMinutes * 100) / totalMinutes;
        return Math.min(100, percentage);
    }


    @Transactional(readOnly = true)
    public String getAudioUrl(Long id) {

        log.debug("Getting audio URL for meditation: {}", id);

        Meditation meditation = meditationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Meditation not found with id: " + id));

        String audioUrl = meditation.getAudioUrl();

        if (audioUrl == null || audioUrl.isBlank()) {
            throw new ResourceNotFoundException(
                    "Audio URL not found for meditation: " + id
            );
        }

        return audioUrl;
    }


}