package com.mental.service;

import com.mental.dto.MeditationSessionRequest;
import com.mental.dto.meditation.MeditationCategoryResponse;
import com.mental.dto.meditation.MeditationDashboardResponse;
import com.mental.dto.meditation.MeditationResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.MeditationMapper;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
import com.mental.repository.MeditationRepository;
import com.mental.repository.MeditationSessionRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MeditationService {

    private final MeditationRepository meditationRepository;
    private final MeditationSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final MeditationMapper meditationMapper;

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
}