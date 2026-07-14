package com.mental.service;

import com.mental.dto.MeditationSessionRequest;
import com.mental.dto.meditation.MeditationCategoryResponse;
import com.mental.dto.meditation.MeditationDashboardResponse;
import com.mental.dto.meditation.MeditationResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
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
                .map(this::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public MeditationResponse getById(Long id){

        Meditation meditation =
                meditationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Meditation not found"
                                ));

        return toResponse(meditation);
    }


    @Transactional(readOnly = true)
    public MeditationDashboardResponse getDashboard(){

        List<Meditation> meditations =
                meditationRepository.findAll();


        List<MeditationResponse> recommended =
                meditations.stream()
                        .map(this::toResponse)
                        .toList();


        MeditationResponse featured =
                recommended.isEmpty()
                        ? null
                        : recommended.get(0);



        List<MeditationCategoryResponse> categories =
                meditations.stream()
                        .map(m ->
                                new MeditationCategoryResponse(
                                        m.getCategory().name(),
                                        "🧘"
                                )
                        )
                        .distinct()
                        .toList();



        return new MeditationDashboardResponse(
                featured,
                categories,
                recommended
        );
    }



    @Transactional
    public void completeSession(
            MeditationSessionRequest request
    ){

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();



        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"+email
                                ));



        Meditation meditation =
                meditationRepository.findById(
                                request.getMeditationId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Meditation not found"
                                ));



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



    @Transactional(readOnly = true)
    public List<MeditationResponse> getHistory(){

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();



        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                ));



        return sessionRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(session ->
                        toResponse(session.getMeditation())
                )
                .toList();
    }



    private MeditationResponse toResponse(
            Meditation meditation
    ){

        return MeditationResponse.builder()
                .id(meditation.getId())
                .title(meditation.getTitle())
                .category(meditation.getCategory().name())
                .duration(meditation.getDuration())
                .description(meditation.getDescription())
                .audioUrl(meditation.getAudioUrl())
                .imageUrl(meditation.getImageUrl())
                .build();
    }

}