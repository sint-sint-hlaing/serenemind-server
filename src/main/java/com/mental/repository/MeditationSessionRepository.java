package com.mental.repository;

import com.mental.model.entity.MeditationSession;
import com.mental.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface MeditationSessionRepository
        extends JpaRepository<MeditationSession,Long> {


    List<MeditationSession> findByUserOrderByCreatedAtDesc(
            User user
    );

}
