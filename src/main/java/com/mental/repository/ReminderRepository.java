package com.mental.repository;

import com.mental.model.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    // Fetch all reminders belonging to a specific user
    List<Reminder> findByUserId(Long userId);

    // Find a specific reminder belonging to a specific user (for security/ownership validation)
    Optional<Reminder> findByIdAndUserId(Long id, Long userId);
}
