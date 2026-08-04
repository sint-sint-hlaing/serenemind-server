package com.mental.repository;

import com.mental.model.entity.StarterPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StarterPromptRepository extends JpaRepository<StarterPrompt, Long> {
}