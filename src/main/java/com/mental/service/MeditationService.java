package com.mental.service;

import com.mental.dto.MeditationHistoryResponse;
import com.mental.dto.MeditationResponse;
import com.mental.dto.MeditationSessionRequest;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
import com.mental.repository.MeditationRepository;
import com.mental.repository.MeditationSessionRepository;
import com.mental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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


    public void completeSession(
            MeditationSessionRequest request,
            String email
    ){


        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                ()-> new RuntimeException("User not found")
                        );



        Meditation meditation =
                meditationRepository.findById(
                                request.getMeditationId()
                        )
                        .orElseThrow(
                                ()-> new RuntimeException("Meditation not found")
                        );



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
    public List<MeditationHistoryResponse> history(
            String email
    ){


        User user =
                userRepository.findByEmail(email)
                        .orElseThrow();



        return sessionRepository
                .findByUserOrderByCreatedAtDesc(user)

                .stream()

                .map(s ->
                        MeditationHistoryResponse.builder()

                                .title(
                                        s.getMeditation().getTitle()
                                )

                                .duration(
                                        s.getMeditation().getDuration()
                                )

                                .completed(
                                        s.isCompleted()
                                )

                                .completedAt(
                                        s.getCompletedAt()
                                )

                                .build()
                )

                .toList();

    }

}
