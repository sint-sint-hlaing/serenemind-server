package com.mental.repository;

import com.mental.model.entity.Journal;
import com.mental.model.entity.JournalAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalAnalysisRepository extends JpaRepository<JournalAnalysis, Long> {

    Optional<JournalAnalysis> findByJournal(Journal journal);

    boolean existsByJournal(Journal journal);
}
