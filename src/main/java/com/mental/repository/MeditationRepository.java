package com.mental.repository;

import com.mental.model.entity.Meditation;
import com.mental.model.entity.enums.MeditationCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeditationRepository
        extends JpaRepository<Meditation,Long> {


    List<Meditation> findByCategory(
            MeditationCategory category
    );

}
