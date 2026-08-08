package com.mental.repository;


import com.mental.model.entity.BreathingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BreathingSessionRepository extends JpaRepository<BreathingSession, String> {

    List<BreathingSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
