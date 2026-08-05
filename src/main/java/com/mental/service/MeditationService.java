package com.mental.service;

import com.mental.dto.MeditationSessionRequest;
import com.mental.dto.meditation.*;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.MeditationMapper;
import com.mental.model.entity.Favorite;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
import com.mental.repository.FavoriteRepository;
import com.mental.repository.MeditationRepository;
import com.mental.repository.MeditationSessionRepository;
import com.mental.repository.UserRepository;
import com.nimbusds.jose.util.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    private final MeditationSessionRepository  meditationSessionRepository;

    @Transactional(readOnly = true)
    public List<MeditationResponse> getAll() {
        log.debug("Fetching all meditations");
        return meditationRepository.findAll()
                .stream()
                .map(meditationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MeditationResponse getById(Long id) {
        log.debug("Fetching meditation by id: {}", id);
        Meditation meditation = meditationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meditation not found with id: " + id));
        return meditationMapper.toResponse(meditation);
    }

    @Transactional(readOnly = true)
    public MeditationDashboardResponse getDashboard() {
        log.debug("Fetching meditation dashboard");

        List<Meditation> meditations = meditationRepository.findAll();

        List<MeditationResponse> recommended = meditations.stream()
                .map(meditationMapper::toResponse)
                .toList();

        MeditationResponse featured = recommended.isEmpty() ? null : recommended.get(0);

        List<MeditationCategoryResponse> categories = meditations.stream()
                .map(m -> new MeditationCategoryResponse(
                        m.getCategories().name(),
                        getCategoryEmoji(m.getCategories().name())
                ))
                .distinct()
                .toList();

        return new MeditationDashboardResponse(featured, categories, recommended);
    }

    @Transactional
    public void completeSession(String email, MeditationSessionRequest request) {
        log.info("Completing meditation session for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Meditation meditation = meditationRepository.findById(request.getMeditationId())
                .orElseThrow(() -> new ResourceNotFoundException("Meditation not found with id: " + request.getMeditationId()));

        MeditationSession session = MeditationSession.builder()
                .user(user)
                .meditation(meditation)
                .completed(request.isCompleted())
                .completedAt(request.isCompleted() ? Instant.now() : null)
                .build();

        sessionRepository.save(session);
        log.info("Meditation session completed successfully for user: {}", email);
    }

    @Transactional(readOnly = true)
    public List<MeditationResponse> getHistory(String email) {
        log.debug("Fetching meditation history for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return sessionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(session -> meditationMapper.toResponse(session.getMeditation()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeditationResponse> getRecommendations(Long userId) {
        log.debug("Fetching recommendations for user: {}", userId);
        return meditationRepository.findRecommendedMeditations(userId)
                .stream()
                .map(meditationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeditationResponse> search(String keyword) {
        log.debug("Searching meditations with keyword: {}", keyword);
        return meditationRepository.searchByKeyword(keyword)
                .stream()
                .map(meditationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeditationResponse> getContinueListening(Long userId) {
        log.debug("Fetching continue listening for user: {}", userId);
        return sessionRepository.findContinueListening(userId)
                .stream()
                .map(meditationMapper::toResponse)
                .toList();
    }

    private String getCategoryEmoji(String category) {
        return switch(category.toUpperCase()) {
            case "MINDFULNESS" -> "🧘";
            case "SLEEP" -> "😴";
            case "ANXIETY" -> "🌿";
            case "FOCUS" -> "🎯";
            case "STRESS" -> "💆";
            default -> "🧘";
        };
    }

    public Resource downloadAudio(Long id) {

        Meditation meditation = meditationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Meditation not found"));

        try {

            Path path = Paths.get(meditation.getAudioUrl());

            org.springframework.core.io.Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Audio file not found.");
            }

            return (Resource) resource;

        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot download audio.", e);
        }
    }

    @Transactional
    public FavoriteResponse toggleFavorite(Long id, Long meditationId) {
        Optional<Favorite> favorite =
                favoriteRepository.findByUserIdAndMeditationId(id, meditationId);

        if (favorite.isPresent()) {

            favoriteRepository.delete(favorite.get());

            return new FavoriteResponse(
                    false,
                    "Removed from favorites");

        }

        User user = userRepository.findById(id)
                .orElseThrow();

        Meditation meditation = meditationRepository.findById(meditationId)
                .orElseThrow();

        Favorite newFavorite = new Favorite();
        newFavorite.setUser(user);
        newFavorite.setMeditation(meditation);

        favoriteRepository.save(newFavorite);

        return new FavoriteResponse(
                true,
                "Added to favorites");
    }

        public TimerResponse saveTimer(
                Long userId,
                Long meditationId,
                TimerRequest request) {


            MeditationSession timer =
                    new MeditationSession();


            timer.setMinutes(request.getMinutes());


            timer.setUser(
                    userRepository.findById(userId)
                            .orElseThrow());


            timer.setMeditation(
                    meditationRepository.findById(meditationId)
                            .orElseThrow());


            meditationSessionRepository.save(timer);


            return new TimerResponse(
                    request.getMinutes(),
                    "Timer saved successfully");
        }

    public ShareResponse getShareLink(Long id) {
        Meditation meditation = meditationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meditation not found"));

        return ShareResponse.builder()
                .title(meditation.getTitle())
                .description(meditation.getDescription())
                .imageUrl(meditation.getImageUrl())
                .shareUrl("https://serenemind.com/meditations/" + meditation.getId())
                .build();
    }

    public MeditationResponse getPrevious(Long id) {
        Meditation meditation =
                meditationRepository
                        .findFirstByIdLessThanOrderByIdDesc(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Previous meditation not found"));

        return meditationMapper.toResponse(meditation);
    }

    public MeditationList getNext(Long id) {


        Meditation meditation =
                meditationRepository
                        .findFirstByIdGreaterThanOrderByIdAsc(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Next meditation not found"));


        return meditationMapper.toListResponse(meditation);
    }

    public Resource stream(Long id) {


        Meditation meditation =
                meditationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Meditation not found"));


        try {

            Path path =
                    Paths.get(meditation.getAudioUrl());


            org.springframework.core.io.Resource resource =
                    new UrlResource(path.toUri());


            if (!resource.exists()) {
                throw new RuntimeException(
                        "Audio file not found");
            }


            return (Resource) resource;


        } catch (MalformedURLException e) {

            throw new RuntimeException(e);
        }
    }
}