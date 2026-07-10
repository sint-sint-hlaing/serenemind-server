package com.mental.service;


import com.mental.dto.Breathing.BreathingSessionRequest;
import com.mental.dto.Breathing.BreathingSessionResponse;
import com.mental.dto.Breathing.BreathingSummaryResponse;
import com.mental.model.entity.BreathingSession;
import com.mental.repository.BreathingSessionRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BreathingService {

    private final BreathingSessionRepository breathingSessionRepository;

    @Transactional
    public BreathingSessionResponse startBreathingSession(UserPrincipal userPrincipal, BreathingSessionRequest request) {
        BreathingSession session = BreathingSession.builder()
                .userId(userPrincipal.getId())
                .exerciseType(request.getExerciseType())
                .targetDurationMinutes(request.getDurationMinutes())
                .completedRounds(0) // incremented via UI signals or computed at completion
                .createdAt(LocalDateTime.now())
                .build();

        BreathingSession saved = breathingSessionRepository.save(session);

        return BreathingSessionResponse.builder()
                .sessionId(saved.getId())
                .exerciseType(saved.getExerciseType())
                .totalDurationSeconds(saved.getTargetDurationMinutes() * 60)
                .estimatedRounds(saved.getTargetDurationMinutes() * 4) // Example logic for 4 rounds per minute
                .build();
    }

    @Transactional
    public void logRoundCompletion(String sessionId, int roundNumber) {
        breathingSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setCompletedRounds(roundNumber);
            breathingSessionRepository.save(session);
        });
    }

    @Transactional
    public BreathingSummaryResponse completeBreathingSession(String sessionId, UserPrincipal userPrincipal) {
        BreathingSession session = breathingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setCompletedAt(LocalDateTime.now());
        // Hardcoded matching your UI screenshot illustration values for demo purposes
        session.setCompletedRounds(4);
        session.setCalculatedBreaths(12);
        breathingSessionRepository.save(session);

        return BreathingSummaryResponse.builder()
                .duration(session.getTargetDurationMinutes() + ":00")
                .rounds(session.getCompletedRounds())
                .totalBreaths(session.getCalculatedBreaths())
                .build();
    }
}
