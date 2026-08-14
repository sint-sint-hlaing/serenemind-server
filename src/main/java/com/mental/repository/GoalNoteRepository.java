package com.mental.repository;

import com.mental.model.entity.GoalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoalNoteRepository extends JpaRepository<GoalNote, Long> {
    List<GoalNote> findByGoalIdOrderByCreatedAtDesc(Long goalId);

    void deleteByGoalId(Long goalId);
}
