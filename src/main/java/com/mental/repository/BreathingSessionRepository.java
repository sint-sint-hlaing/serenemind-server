package com.mental.repository;


import com.mental.model.entity.BreathingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BreathingSessionRepository extends JpaRepository<BreathingSession, String> {

    // Fetch exercise history for a specific user ordered by latest session
    List<BreathingSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
