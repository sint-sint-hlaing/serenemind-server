// UserGoalServiceImpl.java
package com.mental.service.impl;

import com.mental.dto.goal.*;
import com.mental.dto.goal.UserGoal;
import com.mental.exception.ResourceNotFoundException;
import com.mental.exception.ValidationException;
import com.mental.mapper.UserGoalMapper;
import com.mental.model.entity.*;
import com.mental.model.entity.enums.Frequency;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.repository.GoalNoteRepository;
import com.mental.repository.GoalProgressRepository;
import com.mental.repository.UserGoalRepository;
import com.mental.repository.UserRepository;
import com.mental.repository.UserStreakRepository;
import com.mental.service.UserGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserGoalServiceImpl implements UserGoalService {

    private final UserGoalRepository goalRepository;
    private final UserStreakRepository streakRepository;
    private final UserRepository userRepository;
    private final GoalProgressRepository progressRepository;
    private final GoalNoteRepository noteRepository;
    private final UserGoalMapper goalMapper;

    // ===== CREATE GOAL =====
    @Override
    public GoalResponse createGoal(String email, GoalRequest request) {
        log.info("Creating goal for user: {}", email);

        validateGoalRequest(request);
        User user = getUserByEmail(email);

        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : LocalDate.now();

        LocalDate targetDate = startDate.plusDays(request.getTargetDays());
        com.mental.model.entity.UserGoal entity = com.mental.model.entity.UserGoal.builder()
                .user(user)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null
                        ? request.getDescription().trim()
                        : null)
                .frequency(request.getFrequency() != null
                        ? request.getFrequency()
                        : Frequency.DAILY)
                .targetDays(request.getTargetDays())
                .unit(request.getUnit() != null
                        ? request.getUnit()
                        : "days")
                .startDate(startDate)
                .targetDate(targetDate)
                .icon(request.getIcon() != null
                        ? request.getIcon()
                        : "📚")
                .silentMode(request.getSilentMode() != null
                        ? request.getSilentMode()
                        : false)
                .progress(0)
                .status(GoalStatus.ACTIVE)
                .build();

        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Goal created successfully with id: {}", saved.getId());

        // Initialize progress history
        initializeProgress(saved);

        return goalMapper.toResponseDto(saved);
    }

    private void initializeProgress(com.mental.model.entity.UserGoal goal) {
        LocalDate startDate = goal.getStartDate() != null ? goal.getStartDate() : LocalDate.now();
        LocalDate targetDate = goal.getTargetDate() != null ? goal.getTargetDate() : startDate.plusDays(goal.getTargetDays() - 1);

        List<GoalProgress> progressList = new ArrayList<>();
        LocalDate date = startDate;

        while (!date.isAfter(targetDate)) {
            GoalProgress progress = GoalProgress.builder()
                    .goal(goal)
                    .date(date)
                    .completed(false)
                    .value(0.0)
                    .build();

            progressList.add(progress);
            date = date.plusDays(1);
        }

        progressRepository.saveAll(progressList);
    }

    // ===== UPDATE PROGRESS =====
    @Override
    public GoalResponse updateProgress(Long id, String email) {
        log.info("Updating progress for goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);
        validateGoalStatusForUpdate(entity);

        // Check if already updated today
        if (!canUpdateProgress(entity)) {
            log.debug("Progress already updated today for goal: {}", id);
            return goalMapper.toResponseDto(entity);
        }

        // Increment progress
        incrementProgress(entity);


        // Update today's progress
        updateTodayProgress(entity);

        // Check if goal is completed
        if (entity.getProgress() >= entity.getTargetDays()) {
            completeGoalInternal(entity);
        }

        // Update streak count in goal

        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Progress updated for goal: {}, new progress: {}/{}",
                id, entity.getProgress(), entity.getTargetDays());

        return goalMapper.toResponseDto(saved);
    }

    private void updateTodayProgress(com.mental.model.entity.UserGoal entity) {
        LocalDate today = LocalDate.now();
        GoalProgress progress = progressRepository.findByGoalIdAndDate(entity.getId(), today)
                .orElseGet(() -> {
                    GoalProgress newProgress = GoalProgress.builder()
                            .goal(entity)
                            .date(today)
                            .completed(false)
                            .value(0.0)
                            .build();
                    return progressRepository.save(newProgress);
                });

        progress.setCompleted(true);
        progress.setValue(1.0);
        progressRepository.save(progress);
    }

    private int calculateStreak(Long goalId) {
        List<LocalDate> completedDates = progressRepository.findCompletedDatesByGoalId(goalId);
        if (completedDates.isEmpty()) return 0;

        int streak = 0;
        LocalDate today = LocalDate.now();

        // Check if today is completed
        boolean todayCompleted = completedDates.contains(today);
        boolean yesterdayCompleted = completedDates.contains(today.minusDays(1));

        if (!todayCompleted && !yesterdayCompleted) {
            return 0;
        }

        LocalDate checkDate = todayCompleted ? today : today.minusDays(1);

        while (completedDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    // ===== COMPLETE GOAL =====
    @Override
    public GoalResponse completeGoal(Long id, String email) {
        log.info("Completing goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() == GoalStatus.COMPLETED) {
            throw new ValidationException("Goal is already completed");
        }
        if (entity.getStatus() == GoalStatus.CANCELLED) {
            throw new ValidationException("Cannot complete a cancelled goal");
        }
        if (entity.getStatus() == GoalStatus.EXPIRED) {
            throw new ValidationException("Cannot complete an expired goal");
        }

        completeGoalInternal(entity);
        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Goal {} completed successfully!", id);

        return goalMapper.toResponseDto(saved);
    }

    // ===== PAUSE GOAL =====
    @Override
    public GoalResponse pauseGoal(Long id, String email) {
        log.info("Pausing goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() == GoalStatus.COMPLETED) {
            throw new ValidationException("Cannot pause a completed goal");
        }
        if (entity.getStatus() == GoalStatus.CANCELLED) {
            throw new ValidationException("Cannot pause a cancelled goal");
        }
        if (entity.getStatus() == GoalStatus.EXPIRED) {
            throw new ValidationException("Cannot pause an expired goal");
        }
        if (entity.getStatus() == GoalStatus.PAUSED) {
            throw new ValidationException("Goal is already paused");
        }

        entity.setStatus(GoalStatus.PAUSED);
        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Goal {} paused successfully", id);

        return goalMapper.toResponseDto(saved);
    }

    // ===== RESUME GOAL =====
    @Override
    public GoalResponse resumeGoal(Long id, String email) {
        log.info("Resuming goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() != GoalStatus.PAUSED) {
            throw new ValidationException("Goal is not paused");
        }

        entity.setStatus(GoalStatus.ACTIVE);
        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Goal {} resumed successfully", id);

        return goalMapper.toResponseDto(saved);
    }

    // ===== DELETE GOAL (Soft Delete) =====
    @Override
    public void deleteGoal(Long id, String email) {
        log.info("Archiving goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() == GoalStatus.ARCHIVED) {
            throw new ValidationException("Goal is already archived");
        }

        entity.setStatus(GoalStatus.ARCHIVED);
        goalRepository.save(entity);

        log.info("Goal {} archived successfully", id);
    }

    // ===== HARD DELETE GOAL =====
    @Override
    public void hardDeleteGoal(Long id, String email) {
        log.info("Hard deleting goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);

        // Delete associated data
        progressRepository.deleteByGoalId(id);
        noteRepository.deleteByGoalId(id);
        goalRepository.delete(entity);

        log.info("Goal {} hard deleted successfully", id);
    }

    // ===== GET ALL GOALS =====
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getUserGoals(String email) {
        log.debug("Fetching all goals for user: {}", email);

        User user = getUserByEmail(email);
        List<com.mental.model.entity.UserGoal> entities = goalRepository.findByUser(user);

        return entities.stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ===== GET ACTIVE GOALS =====
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getActiveGoals(String email) {
        log.debug("Fetching active goals for user: {}", email);

        User user = getUserByEmail(email);

        return goalRepository.findByUserAndStatusIn(user,
                        List.of(GoalStatus.ACTIVE, GoalStatus.PAUSED))
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ===== GET COMPLETED GOALS =====
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getCompletedGoals(String email) {
        log.debug("Fetching completed goals for user: {}", email);

        User user = getUserByEmail(email);

        return goalRepository.findByUserAndStatus(user, GoalStatus.COMPLETED)
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ===== GET GOALS BY STATUS =====
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsByStatus(String email, GoalStatus status) {
        log.debug("Fetching goals for user: {} with status: {}", email, status);

        User user = getUserByEmail(email);

        return goalRepository.findByUserAndStatus(user, status)
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }
    // ===== GET GOALS FOR DASHBOARD =====
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsForDashboard(String email) {
        log.debug("Fetching dashboard goals for user: {}", email);
        User user = getUserByEmail(email);

        return goalRepository.findTop5ActiveByUser(user)
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ===== GET STATISTICS =====
    @Override
    @Transactional(readOnly = true)
    public GoalStatistics getGoalStatistics(String email) {
        log.debug("Fetching goal statistics for user: {}", email);

        User user = getUserByEmail(email);

        long total = goalRepository.countByUser(user);
        long active = goalRepository.countByUserAndStatus(user, GoalStatus.ACTIVE);
        long paused = goalRepository.countByUserAndStatus(user, GoalStatus.PAUSED);
        long completed = goalRepository.countByUserAndStatus(user, GoalStatus.COMPLETED);
        long expired = goalRepository.countByUserAndStatus(user, GoalStatus.EXPIRED);
        long cancelled = goalRepository.countByUserAndStatus(user, GoalStatus.CANCELLED);

        long totalProgress = calculateTotalProgress(user);


        return GoalStatistics.builder()
                .total(total)
                .active(active)
                .paused(paused)
                .completed(completed)
                .expired(expired)
                .cancelled(cancelled)
                .totalProgress(totalProgress)
                .completionRate(total > 0 ? (completed * 100.0) / total : 0.0)
                .build();
    }


    // ===== NOTE MANAGEMENT =====
    @Override
    @Transactional
    public GoalNoteDTO addNote(Long goalId, String email, String content) {
        log.info("Adding note to goal: {} by user: {}", goalId, email);

        com.mental.model.entity.UserGoal goal = getGoalAndValidateOwnership(goalId, email);

        GoalNote note = GoalNote.builder()
                .goal(goal)
                .content(content)
                .build();

        note = noteRepository.save(note);
        return goalMapper.toNoteDto(note);
    }

    @Override
    @Transactional
    public void deleteNote(Long noteId, String email) {
        log.info("Deleting note: {} by user: {}", noteId, email);

        GoalNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        // Validate ownership
        if (!note.getGoal().getUser().getEmail().equals(email)) {
            throw new SecurityException("Unauthorized to delete this note");
        }

        noteRepository.delete(note);
        log.info("Note {} deleted successfully", noteId);
    }

    @Override
    @Transactional
    public GoalNoteDTO updateNote(Long noteId, String email, String content) {
        log.info("Updating note: {} by user: {}", noteId, email);

        GoalNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (!note.getGoal().getUser().getEmail().equals(email)) {
            throw new SecurityException("Unauthorized to update this note");
        }

        note.setContent(content);
        note = noteRepository.save(note);
        return goalMapper.toNoteDto(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalNoteDTO> getNotesByGoal(Long goalId, String email) {
        log.debug("Fetching notes for goal: {} by user: {}", goalId, email);

        // Validate ownership
        getGoalAndValidateOwnership(goalId, email);

        return noteRepository.findByGoalIdOrderByCreatedAtDesc(goalId)
                .stream()
                .map(goalMapper::toNoteDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id, String email) {
        log.debug("Fetching goal: {} for user: {}", id, email);

        User user = getUserByEmail(email);

        com.mental.model.entity.UserGoal goal = goalRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Goal not found with id: " + id
                        )
                );

        // Make sure the goal belongs to the current user
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException(
                    "Goal not found with id: " + id
            );
        }

        return goalMapper.toResponseDto(goal);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(Long id, String email, GoalRequest request) {
        log.info("Updating goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);
        validateGoalRequest(request);

        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription().trim());
        }
        if (request.getFrequency() != null) {
            entity.setFrequency(request.getFrequency());
        }
        if (request.getTargetDays() > 0) {
            entity.setTargetDays(request.getTargetDays());
            if (entity.getStartDate() != null) {
                entity.setTargetDate(entity.getStartDate().plusDays(request.getTargetDays()));
            }
        }
        if (request.getUnit() != null) {
            entity.setUnit(request.getUnit());
        }
        if (request.getStartDate() != null) {
            entity.setStartDate(request.getStartDate());
            entity.setTargetDate(request.getStartDate().plusDays(entity.getTargetDays()));
        }
        if (request.getIcon() != null) {
            entity.setIcon(request.getIcon());
        }
        if (request.getSilentMode() != null) {
            entity.setSilentMode(request.getSilentMode());
        }

        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Goal updated successfully with id: {}", saved.getId());

        return goalMapper.toResponseDto(saved);
    }


    // ===== SCHEDULED JOB =====
    @Override
    @Transactional
    public void checkExpiredGoals() {
        log.info("Checking for expired goals");

        LocalDate today = LocalDate.now();
        List<com.mental.model.entity.UserGoal> expiredGoals =
                goalRepository.findByStatusAndTargetDateBefore(GoalStatus.ACTIVE, today);

        int count = 0;
        for (com.mental.model.entity.UserGoal entity : expiredGoals) {
            log.info("Goal {} has expired", entity.getId());
            entity.setStatus(GoalStatus.EXPIRED);
            goalRepository.save(entity);
            count++;
        }

        log.info("Expired goals check completed. Found: {}", count);
    }

    // ===== PRIVATE HELPER METHODS =====
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private com.mental.model.entity.UserGoal getGoalAndValidateOwnership(Long goalId, String email) {
        com.mental.model.entity.UserGoal entity = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + goalId));

        if (!entity.getUser().getEmail().equals(email)) {
            throw new SecurityException("Unauthorized to access this goal");
        }

        return entity;
    }

    private void validateGoalRequest(GoalRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }
        if (request.getTitle().length() > 200) {
            throw new ValidationException("Title must be less than 200 characters");
        }
        if (request.getTargetDays() < 1) {
            throw new ValidationException("Target days must be at least 1");
        }
        if (request.getTargetDays() > 365) {
            throw new ValidationException("Target days cannot exceed 365 days");
        }
    }

    private void validateGoalStatusForUpdate(com.mental.model.entity.UserGoal entity) {
        if (entity.getStatus() == GoalStatus.COMPLETED) {
            throw new ValidationException("Goal is already completed");
        }
        if (entity.getStatus() == GoalStatus.CANCELLED) {
            throw new ValidationException("Goal has been cancelled");
        }
        if (entity.getStatus() == GoalStatus.EXPIRED) {
            throw new ValidationException("Goal has expired");
        }
        if (entity.getStatus() == GoalStatus.ARCHIVED) {
            throw new ValidationException("Goal has been archived");
        }
    }

    private boolean canUpdateProgress(com.mental.model.entity.UserGoal entity) {
        if (entity.getUpdatedAt() == null) {
            return true;
        }
        LocalDate updatedDate = entity.getUpdatedAt().toLocalDate();
        LocalDate today = LocalDate.now();
        return !updatedDate.equals(today);
    }

    private void incrementProgress(com.mental.model.entity.UserGoal entity) {
        if (entity.getProgress() < entity.getTargetDays()) {
            entity.setProgress(entity.getProgress() + 1);
        }
    }

    private void completeGoalInternal(com.mental.model.entity.UserGoal entity) {
        entity.setProgress(entity.getTargetDays());
        entity.setStatus(GoalStatus.COMPLETED);
        entity.setCompletedAt(LocalDate.now());
        log.info("Goal {} completed!", entity.getId());
    }

    private long calculateTotalProgress(User user) {
        List<com.mental.model.entity.UserGoal> activeGoals =
                goalRepository.findByUserAndStatus(user, GoalStatus.ACTIVE);
        return activeGoals.stream()
                .mapToLong(g -> (g.getProgress() * 100L) / g.getTargetDays())
                .sum();
    }


    // ===== UPDATE PROGRESS WITH UI PAYLOAD =====
    @Override
    public GoalResponse updateProgress(Long id, String email, ProgressUpdateRequest request) {
        log.info("Updating progress for goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);
        validateGoalStatusForUpdate(entity);

        // Update today's progress entry based on user selection (Completed/Skipped)
        updateTodayProgressWithDetails(entity, request.isCompleted());

        // Adjust overall goal progress counter if completed today
        if (request.isCompleted()) {
            if (canUpdateProgress(entity)) {
                incrementProgress(entity);
            }
        }

        // Add optional note if provided and not empty
        if (request.getNote() != null && !request.getNote().trim().isEmpty()) {
            GoalNote note = GoalNote.builder()
                    .goal(entity)
                    .content(request.getNote().trim())
                    .build();
            noteRepository.save(note);
        }

        // Check if goal is completed
        if (entity.getProgress() >= entity.getTargetDays()) {
            completeGoalInternal(entity);
        }

        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Progress updated for goal: {}, new progress: {}/{}",
                id, entity.getProgress(), entity.getTargetDays());

        return goalMapper.toResponseDto(saved);
    }

    private void updateTodayProgressWithDetails(com.mental.model.entity.UserGoal entity, boolean isCompleted) {
        LocalDate today = LocalDate.now();
        GoalProgress progress = progressRepository.findByGoalIdAndDate(entity.getId(), today)
                .orElseGet(() -> {
                    GoalProgress newProgress = GoalProgress.builder()
                            .goal(entity)
                            .date(today)
                            .completed(false)
                            .value(0.0)
                            .build();
                    return progressRepository.save(newProgress);
                });

        progress.setCompleted(isCompleted);
        progress.setValue(isCompleted ? 1.0 : 0.0);
        progressRepository.save(progress);
    }



}