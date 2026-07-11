package com.mental.service;

import com.mental.dto.MeditationHistoryResponse;
import com.mental.dto.MeditationResponse;
import com.mental.dto.MeditationSessionRequest;
import com.mental.dto.Reminder.MeditationDto;
import com.mental.dto.meditation.MeditationCategoryResponse;
import com.mental.dto.meditation.MeditationDashboardResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
import com.mental.model.entity.enums.MeditationCategory;
import com.mental.repository.MeditationRepository;
import com.mental.repository.MeditationSessionRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MeditationService {


    private final MeditationRepository meditationRepository;

    private final MeditationSessionRepository sessionRepository;

    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public List<MeditationResponse> getAll(){

        return meditationRepository.findAll()
                .stream()
                .map(m -> MeditationResponse.builder()

                        .id(m.getId())
                        .title(m.getTitle())
                        .category(m.getCategory().name())
                        .duration(m.getDuration())
                        .audioUrl(m.getAudioUrl())
                        .description(m.getDescription())

                        .build())

                .toList();

    }

    @Transactional(readOnly = true)
    public MeditationResponse getById(Long id){

        Meditation meditation =
                meditationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Meditation not found"));

        return toResponse(meditation);
    }

    public MeditationDashboardResponse getDashboard() {

        List<Meditation> meditations = meditationRepository.findAll();

        List<MeditationResponse> recommended = meditations.stream()
                .map(this::toResponse)
                .toList();

        MeditationResponse featured =
                recommended.isEmpty()
                        ? null
                        : recommended.get(0);
        List<MeditationCategoryResponse> categories = meditations.stream()
                .map(m -> new MeditationCategoryResponse(
                        m.getCategory().name(),
                        "🧘"
                ))
                .distinct()
                .toList();

        return new MeditationDashboardResponse(
                featured,
                categories,
                recommended
        );
    }
    public void completeSession(MeditationSessionRequest request){

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("User not found"));

        Meditation meditation =
                meditationRepository.findById(request.getMeditationId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Meditation not found"));

        MeditationSession session =
                MeditationSession.builder()
                        .user(user)
                        .meditation(meditation)
                        .completed(request.isCompleted())
                        .completedAt(
                                request.isCompleted()
                                        ? Instant.now()
                                        : null
                        )
                        .build();

        sessionRepository.save(session);
    }

    private MeditationResponse toResponse(Meditation meditation) {

        return MeditationResponse.builder()
                .id(meditation.getId())
                .title(meditation.getTitle())
                .category(meditation.getCategory().name())
                .duration(meditation.getDuration())
                .audioUrl(meditation.getAudioUrl())
                .imgUrl(meditation.getImageUrl())
                .description(meditation.getDescription())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MeditationResponse> getHistory() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return sessionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(session -> toResponse(session.getMeditation()))
                .toList();
    }
}
