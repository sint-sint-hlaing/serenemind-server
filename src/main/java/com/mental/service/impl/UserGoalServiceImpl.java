package com.mental.service.impl;

import com.mental.dto.goal.GoalNoteDTO;
import com.mental.dto.goal.GoalRequest;
import com.mental.dto.goal.GoalResponse;
import com.mental.dto.goal.GoalStatistics;
import com.mental.dto.goal.ProgressUpdateRequest;
import com.mental.exception.ResourceNotFoundException;
import com.mental.exception.ValidationException;
import com.mental.mapper.UserGoalMapper;
import com.mental.model.entity.GoalNote;
import com.mental.model.entity.GoalProgress;
import com.mental.model.entity.User;
import com.mental.model.entity.UserGoal;
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
import java.time.temporal.ChronoUnit;
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

    // =========================================================
    // CREATE GOAL
    // =========================================================

    @Override
    public GoalResponse createGoal(String email, GoalRequest request) {

        log.info("Creating goal for user: {}", email);

        validateGoalRequest(request);

        User user = getUserByEmail(email);

        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : LocalDate.now();

        Frequency frequency = request.getFrequency() != null
                ? request.getFrequency()
                : Frequency.DAILY;

        int targetDays = request.getTargetDays();

        /*
         * targetDays means TOTAL GOAL DURATION.
         *
         * Example:
         * targetDays = 5
         *
         * Aug 16 = Day 1
         * Aug 17 = Day 2
         * Aug 18 = Day 3
         * Aug 19 = Day 4
         * Aug 20 = Day 5
         */
        LocalDate targetDate = startDate.plusDays(targetDays - 1);

        com.mental.model.entity.UserGoal entity =
                com.mental.model.entity.UserGoal.builder()
                        .user(user)
                        .title(request.getTitle().trim())
                        .description(
                                request.getDescription() != null
                                        ? request.getDescription().trim()
                                        : null
                        )
                        .frequency(frequency)
                        .targetDays(targetDays)
                        .unit(
                                request.getUnit() != null
                                        ? request.getUnit().trim()
                                        : "days"
                        )
                        .startDate(startDate)
                        .targetDate(targetDate)
                        .icon(
                                request.getIcon() != null
                                        ? request.getIcon()
                                        : "📚"
                        )
                        .silentMode(
                                request.getSilentMode() != null
                                        && request.getSilentMode()
                        )
                        .progress(0)
                        .status(GoalStatus.ACTIVE)
                        .build();

        com.mental.model.entity.UserGoal saved =
                goalRepository.save(entity);

        log.info(
                "Goal created successfully: id={}, startDate={}, targetDate={}, targetDays={}, frequency={}",
                saved.getId(),
                saved.getStartDate(),
                saved.getTargetDate(),
                saved.getTargetDays(),
                saved.getFrequency()
        );

        /*
         * Create history for each day in the goal duration.
         */
        initializeProgress(saved);

        return goalMapper.toResponseDto(saved);
    }


    private void initializeProgress(
            com.mental.model.entity.UserGoal goal) {

        LocalDate startDate = goal.getStartDate();
        LocalDate targetDate = goal.getTargetDate();

        if (startDate == null || targetDate == null) {
            throw new ValidationException(
                    "Goal start date and target date are required"
            );
        }

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

        if (!progressList.isEmpty()) {
            progressRepository.saveAll(progressList);
        }
    }
    // =========================================================
    // UPDATE PROGRESS - SIMPLE API
    // =========================================================

    @Override
    public GoalResponse updateProgress(Long id, String email) {

        log.info(
                "Updating progress for goal: {} by user: {}",
                id,
                email
        );

        UserGoal entity = getGoalAndValidateOwnership(id, email);

        validateGoalStatusForUpdate(entity);

        LocalDate today = LocalDate.now();

        validateGoalDate(entity, today);

        /*
         * Check whether the current period has already
         * been completed.
         */
        if (isCurrentPeriodCompleted(entity, today)) {

            log.debug(
                    "Current period already completed for goal {}",
                    id
            );

            return goalMapper.toResponseDto(entity);
        }

        /*
         * Create today's progress record.
         *
         * For WEEKLY / MONTHLY goals, the actual completion
         * date is still stored. Period validation is handled
         * separately.
         */
        GoalProgress progress = getOrCreateProgress(
                entity,
                today
        );

        progress.setCompleted(true);
        progress.setValue(1.0);

        progressRepository.save(progress);

        /*
         * Increase goal progress by one completed period.
         */
        incrementProgress(entity);

        /*
         * If target reached -> COMPLETED.
         */
        if (entity.getProgress() >= entity.getTargetDays()) {
            completeGoalInternal(entity);
        }

        UserGoal saved = goalRepository.save(entity);

        log.info(
                "Goal {} progress updated: {}/{}",
                id,
                saved.getProgress(),
                saved.getTargetDays()
        );

        return goalMapper.toResponseDto(saved);
    }

    // =========================================================
    // UPDATE PROGRESS WITH UI PAYLOAD
    // =========================================================

    @Override
    public GoalResponse updateProgress(
            Long id,
            String email,
            ProgressUpdateRequest request
    ) {

        log.info(
                "Updating progress for goal: {} by user: {}",
                id,
                email
        );

        if (request == null) {
            throw new ValidationException(
                    "Progress update request is required"
            );
        }

        UserGoal entity = getGoalAndValidateOwnership(id, email);

        validateGoalStatusForUpdate(entity);

        LocalDate today = LocalDate.now();

        validateGoalDate(entity, today);

        /*
         * Get today's progress record.
         *
         * We intentionally use today's date because the user
         * is modifying today's UI checkbox.
         */
        GoalProgress progress = progressRepository
                .findByGoalIdAndDate(entity.getId(), today)
                .orElse(null);

        boolean wasCompleted = progress != null
                && Boolean.TRUE.equals(progress.getCompleted());

        boolean nowCompleted = request.isCompleted();

        /*
         * -----------------------------------------------------
         * COMPLETE
         * -----------------------------------------------------
         */
        if (nowCompleted && !wasCompleted) {

            /*
             * Prevent completing the same WEEKLY / MONTHLY
             * period more than once.
             */
            if (isCurrentPeriodCompletedExcludingToday(
                    entity,
                    today
            )) {

                throw new ValidationException(
                        "This goal period has already been completed"
                );
            }

            if (progress == null) {
                progress = GoalProgress.builder()
                        .goal(entity)
                        .date(today)
                        .completed(false)
                        .value(0.0)
                        .build();
            }

            progress.setCompleted(true);
            progress.setValue(1.0);

            progressRepository.save(progress);

            incrementProgress(entity);

            log.debug(
                    "Goal {} completed for current period. progress={}/{}",
                    id,
                    entity.getProgress(),
                    entity.getTargetDays()
            );
        }

        /*
         * -----------------------------------------------------
         * UN-COMPLETE
         * -----------------------------------------------------
         */
        else if (!nowCompleted && wasCompleted) {

            /*
             * Only today's completion can be undone from
             * today's UI.
             */
            progress.setCompleted(false);
            progress.setValue(0.0);

            progressRepository.save(progress);

            decrementProgress(entity);

            log.debug(
                    "Goal {} completion undone. progress={}/{}",
                    id,
                    entity.getProgress(),
                    entity.getTargetDays()
            );
        }

        /*
         * -----------------------------------------------------
         * NO CHANGE
         * -----------------------------------------------------
         */
        else if (progress == null) {

            /*
             * If UI sends false for a day that has never
             * been completed, don't create unnecessary records.
             */
            log.debug(
                    "No progress change for goal {}",
                    id
            );
        }

        /*
         * -----------------------------------------------------
         * NOTE
         * -----------------------------------------------------
         */
        if (request.getNote() != null
                && !request.getNote().trim().isEmpty()) {

            GoalNote note = GoalNote.builder()
                    .goal(entity)
                    .content(request.getNote().trim())
                    .build();

            noteRepository.save(note);
        }

        /*
         * -----------------------------------------------------
         * COMPLETE GOAL
         * -----------------------------------------------------
         */
        if (entity.getProgress() >= entity.getTargetDays()) {

            completeGoalInternal(entity);

        } else if (
                entity.getStatus() == GoalStatus.COMPLETED
        ) {

            /*
             * This happens when the user unchecks a completed
             * period after the goal was previously completed.
             */
            entity.setStatus(GoalStatus.ACTIVE);
            entity.setCompletedAt(null);

            log.info(
                    "Goal {} changed from COMPLETED back to ACTIVE",
                    id
            );
        }

        UserGoal saved = goalRepository.save(entity);

        return goalMapper.toResponseDto(saved);
    }

    // =========================================================
    // COMPLETE GOAL MANUALLY
    // =========================================================

    @Override
    public GoalResponse completeGoal(Long id, String email) {

        log.info(
                "Manually completing goal: {} by user: {}",
                id,
                email
        );

        UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() == GoalStatus.COMPLETED) {
            throw new ValidationException(
                    "Goal is already completed"
            );
        }

        if (entity.getStatus() == GoalStatus.CANCELLED) {
            throw new ValidationException(
                    "Cannot complete a cancelled goal"
            );
        }

        if (entity.getStatus() == GoalStatus.EXPIRED) {
            throw new ValidationException(
                    "Cannot complete an expired goal"
            );
        }

        if (entity.getStatus() == GoalStatus.ARCHIVED) {
            throw new ValidationException(
                    "Cannot complete an archived goal"
            );
        }

        completeGoalInternal(entity);

        UserGoal saved = goalRepository.save(entity);

        log.info(
                "Goal {} manually completed",
                id
        );

        return goalMapper.toResponseDto(saved);
    }

    // =========================================================
    // PAUSE GOAL
    // =========================================================

    @Override
    public GoalResponse pauseGoal(Long id, String email) {

        log.info(
                "Pausing goal: {} by user: {}",
                id,
                email
        );

        UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() == GoalStatus.COMPLETED) {
            throw new ValidationException(
                    "Cannot pause a completed goal"
            );
        }

        if (entity.getStatus() == GoalStatus.CANCELLED) {
            throw new ValidationException(
                    "Cannot pause a cancelled goal"
            );
        }

        if (entity.getStatus() == GoalStatus.EXPIRED) {
            throw new ValidationException(
                    "Cannot pause an expired goal"
            );
        }

        if (entity.getStatus() == GoalStatus.ARCHIVED) {
            throw new ValidationException(
                    "Cannot pause an archived goal"
            );
        }

        if (entity.getStatus() == GoalStatus.PAUSED) {
            throw new ValidationException(
                    "Goal is already paused"
            );
        }

        entity.setStatus(GoalStatus.PAUSED);

        UserGoal saved = goalRepository.save(entity);

        log.info(
                "Goal {} paused successfully",
                id
        );

        return goalMapper.toResponseDto(saved);
    }

    // =========================================================
    // RESUME GOAL
    // =========================================================

    @Override
    public GoalResponse resumeGoal(Long id, String email) {

        log.info(
                "Resuming goal: {} by user: {}",
                id,
                email
        );

        UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() != GoalStatus.PAUSED) {
            throw new ValidationException(
                    "Goal is not paused"
            );
        }

        LocalDate today = LocalDate.now();

        /*
         * If target date has already passed while paused,
         * don't allow resume into an expired goal.
         */
        if (today.isAfter(entity.getTargetDate())) {

            entity.setStatus(GoalStatus.EXPIRED);

            goalRepository.save(entity);

            throw new ValidationException(
                    "Cannot resume an expired goal"
            );
        }

        entity.setStatus(GoalStatus.ACTIVE);

        UserGoal saved = goalRepository.save(entity);

        log.info(
                "Goal {} resumed successfully",
                id
        );

        return goalMapper.toResponseDto(saved);
    }

    // =========================================================
    // SOFT DELETE / ARCHIVE
    // =========================================================

    @Override
    public void deleteGoal(Long id, String email) {

        log.info(
                "Archiving goal: {} by user: {}",
                id,
                email
        );

        UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() == GoalStatus.ARCHIVED) {
            throw new ValidationException(
                    "Goal is already archived"
            );
        }

        entity.setStatus(GoalStatus.ARCHIVED);

        goalRepository.save(entity);

        log.info(
                "Goal {} archived successfully",
                id
        );
    }

    // =========================================================
    // HARD DELETE
    // =========================================================

    @Override
    public void hardDeleteGoal(Long id, String email) {

        log.info(
                "Hard deleting goal: {} by user: {}",
                id,
                email
        );

        UserGoal entity = getGoalAndValidateOwnership(id, email);

        /*
         * Delete children first to avoid FK constraint issues.
         */
        progressRepository.deleteByGoalId(id);
        noteRepository.deleteByGoalId(id);

        goalRepository.delete(entity);

        log.info(
                "Goal {} hard deleted successfully",
                id
        );
    }

    // =========================================================
    // GET ALL GOALS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getUserGoals(String email) {

        log.debug(
                "Fetching all goals for user: {}",
                email
        );

        User user = getUserByEmail(email);

        return goalRepository
                .findByUser(user)
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET ACTIVE GOALS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getActiveGoals(String email) {

        log.debug(
                "Fetching active goals for user: {}",
                email
        );

        User user = getUserByEmail(email);

        return goalRepository
                .findByUserAndStatusIn(
                        user,
                        List.of(
                                GoalStatus.ACTIVE,
                                GoalStatus.PAUSED
                        )
                )
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET COMPLETED GOALS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getCompletedGoals(String email) {

        log.debug(
                "Fetching completed goals for user: {}",
                email
        );

        User user = getUserByEmail(email);

        return goalRepository
                .findByUserAndStatus(
                        user,
                        GoalStatus.COMPLETED
                )
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET GOALS BY STATUS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsByStatus(
            String email,
            GoalStatus status
    ) {

        log.debug(
                "Fetching goals for user: {} with status: {}",
                email,
                status
        );

        if (status == null) {
            throw new ValidationException(
                    "Goal status is required"
            );
        }

        User user = getUserByEmail(email);

        return goalRepository
                .findByUserAndStatus(user, status)
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsForDashboard(
            String email
    ) {

        log.debug(
                "Fetching dashboard goals for user: {}",
                email
        );

        User user = getUserByEmail(email);

        return goalRepository
                .findTop5ActiveByUser(user)
                .stream()
                .map(goalMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public GoalStatistics getGoalStatistics(
            String email
    ) {

        log.debug(
                "Fetching goal statistics for user: {}",
                email
        );

        User user = getUserByEmail(email);

        long total =
                goalRepository.countByUser(user);

        long active =
                goalRepository.countByUserAndStatus(
                        user,
                        GoalStatus.ACTIVE
                );

        long paused =
                goalRepository.countByUserAndStatus(
                        user,
                        GoalStatus.PAUSED
                );

        long completed =
                goalRepository.countByUserAndStatus(
                        user,
                        GoalStatus.COMPLETED
                );

        long expired =
                goalRepository.countByUserAndStatus(
                        user,
                        GoalStatus.EXPIRED
                );

        long cancelled =
                goalRepository.countByUserAndStatus(
                        user,
                        GoalStatus.CANCELLED
                );

        double totalProgress =
                calculateTotalProgress(user);

        double completionRate =
                total > 0
                        ? (completed * 100.0) / total
                        : 0.0;

        return GoalStatistics.builder()
                .total(total)
                .active(active)
                .paused(paused)
                .completed(completed)
                .expired(expired)
                .cancelled(cancelled)
                .totalProgress(Math.round(totalProgress))
                .completionRate(completionRate)
                .build();
    }

    // =========================================================
    // NOTES
    // =========================================================

    @Override
    public GoalNoteDTO addNote(
            Long goalId,
            String email,
            String content
    ) {

        log.info(
                "Adding note to goal: {} by user: {}",
                goalId,
                email
        );

        UserGoal goal =
                getGoalAndValidateOwnership(
                        goalId,
                        email
                );

        validateNoteContent(content);

        GoalNote note = GoalNote.builder()
                .goal(goal)
                .content(content.trim())
                .build();

        GoalNote saved =
                noteRepository.save(note);

        return goalMapper.toNoteDto(saved);
    }

    @Override
    public void deleteNote(
            Long noteId,
            String email
    ) {

        log.info(
                "Deleting note: {} by user: {}",
                noteId,
                email
        );

        GoalNote note =
                noteRepository.findById(noteId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Note not found with id: " + noteId
                                )
                        );

        validateNoteOwnership(note, email);

        noteRepository.delete(note);

        log.info(
                "Note {} deleted successfully",
                noteId
        );
    }

    @Override
    public GoalNoteDTO updateNote(
            Long noteId,
            String email,
            String content
    ) {

        log.info(
                "Updating note: {} by user: {}",
                noteId,
                email
        );

        GoalNote note =
                noteRepository.findById(noteId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Note not found with id: " + noteId
                                )
                        );

        validateNoteOwnership(note, email);
        validateNoteContent(content);

        note.setContent(content.trim());

        GoalNote saved =
                noteRepository.save(note);

        return goalMapper.toNoteDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalNoteDTO> getNotesByGoal(
            Long goalId,
            String email
    ) {

        log.debug(
                "Fetching notes for goal: {} by user: {}",
                goalId,
                email
        );

        getGoalAndValidateOwnership(
                goalId,
                email
        );

        return noteRepository
                .findByGoalIdOrderByCreatedAtDesc(goalId)
                .stream()
                .map(goalMapper::toNoteDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET GOAL BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoalById(
            Long id,
            String email
    ) {

        log.debug(
                "Fetching goal: {} for user: {}",
                id,
                email
        );

        UserGoal goal =
                getGoalAndValidateOwnership(
                        id,
                        email
                );

        return goalMapper.toResponseDto(goal);
    }

    // =========================================================
    // UPDATE GOAL
    // =========================================================

    @Override
    public GoalResponse updateGoal(
            Long id,
            String email,
            GoalRequest request
    ) {

        log.info(
                "Updating goal: {} by user: {}",
                id,
                email
        );

        if (request == null) {
            throw new ValidationException(
                    "Goal request is required"
            );
        }

        UserGoal entity =
                getGoalAndValidateOwnership(
                        id,
                        email
                );

        validateGoalCanBeEdited(entity);

        validateGoalRequest(request);

        /*
         * -----------------------------------------------------
         * BASIC FIELDS
         * -----------------------------------------------------
         */

        if (request.getTitle() != null) {

            String title =
                    request.getTitle().trim();

            if (title.isEmpty()) {
                throw new ValidationException(
                        "Title cannot be empty"
                );
            }

            entity.setTitle(title);
        }

        if (request.getDescription() != null) {
            entity.setDescription(
                    normalizeNullable(
                            request.getDescription()
                    )
            );
        }

        if (request.getIcon() != null) {
            entity.setIcon(request.getIcon());
        }

        if (request.getUnit() != null) {

            String unit =
                    request.getUnit().trim();

            if (unit.isEmpty()) {
                throw new ValidationException(
                        "Unit cannot be empty"
                );
            }

            entity.setUnit(unit);
        }

        if (request.getSilentMode() != null) {
            entity.setSilentMode(
                    request.getSilentMode()
            );
        }

        /*
         * -----------------------------------------------------
         * STRUCTURAL FIELDS
         * -----------------------------------------------------
         */

        Frequency newFrequency =
                request.getFrequency() != null
                        ? request.getFrequency()
                        : entity.getFrequency();

        int newTargetDays =
                request.getTargetDays() > 0
                        ? request.getTargetDays()
                        : entity.getTargetDays();

        LocalDate newStartDate =
                request.getStartDate() != null
                        ? request.getStartDate()
                        : entity.getStartDate();

        boolean structureChanged =
                newFrequency != entity.getFrequency()
                        || newTargetDays != entity.getTargetDays()
                        || !newStartDate.equals(entity.getStartDate());

        /*
         * Once progress exists, changing the goal structure
         * would make existing progress history ambiguous.
         */
        if (structureChanged && entity.getProgress() > 0) {

            throw new ValidationException(
                    "Frequency, target, or start date cannot be changed after progress has started"
            );
        }

        if (request.getStartDate() != null) {
            validateStartDate(
                    request.getStartDate()
            );
        }

        /*
         * Apply structural changes.
         */
        entity.setFrequency(newFrequency);
        entity.setTargetDays(newTargetDays);
        entity.setStartDate(newStartDate);

        /*
         * ALWAYS recalculate targetDate.
         *
         * This fixes the original bug where updateGoal()
         * always used plusDays().
         */
        entity.setTargetDate(
                calculateTargetDate(
                        newStartDate,
                        newFrequency,
                        newTargetDays
                )
        );

        UserGoal saved =
                goalRepository.save(entity);

        log.info(
                "Goal {} updated successfully. frequency={}, target={}, start={}, targetDate={}",
                saved.getId(),
                saved.getFrequency(),
                saved.getTargetDays(),
                saved.getStartDate(),
                saved.getTargetDate()
        );

        return goalMapper.toResponseDto(saved);
    }

    // =========================================================
    // EXPIRED GOALS
    // =========================================================

    @Override
    public void checkExpiredGoals() {

        log.info(
                "Checking for expired goals"
        );

        LocalDate today =
                LocalDate.now();

        List<UserGoal> expiredGoals =
                goalRepository.findByStatusAndTargetDateBefore(
                        GoalStatus.ACTIVE,
                        today
                );

        int count = 0;

        for (UserGoal entity : expiredGoals) {

            /*
             * Don't expire a goal that already reached
             * its target.
             */
            if (entity.getProgress() >= entity.getTargetDays()) {

                completeGoalInternal(entity);

                goalRepository.save(entity);

                continue;
            }

            entity.setStatus(
                    GoalStatus.EXPIRED
            );

            goalRepository.save(entity);

            count++;

            log.info(
                    "Goal {} marked as EXPIRED",
                    entity.getId()
            );
        }

        log.info(
                "Expired goals check completed. Found: {}",
                count
        );
    }

    // =========================================================
    // PRIVATE: TARGET DATE
    // =========================================================

    private LocalDate calculateTargetDate(
            LocalDate startDate,
            Frequency frequency,
            int targetPeriods
    ) {

        if (startDate == null) {
            throw new ValidationException(
                    "Start date is required"
            );
        }

        if (targetPeriods < 1) {
            throw new ValidationException(
                    "Target must be at least 1"
            );
        }

        /*
         * Inclusive period calculation.
         *
         * DAILY + 1  -> same day
         * DAILY + 30 -> start + 29 days
         *
         * WEEKLY + 1 -> same week
         * WEEKLY + 4 -> start + 3 weeks
         *
         * MONTHLY + 1 -> same month
         * MONTHLY + 3 -> start + 2 months
         */
        return switch (frequency) {

            case DAILY ->
                    startDate.plusDays(
                            targetPeriods - 1L
                    );

            case WEEKLY ->
                    startDate.plusWeeks(
                            targetPeriods - 1L
                    );

            case MONTHLY ->
                    startDate.plusMonths(
                            targetPeriods - 1L
                    );
        };
    }

    // =========================================================
    // PRIVATE: PROGRESS
    // =========================================================

    private GoalProgress getOrCreateProgress(
            UserGoal entity,
            LocalDate date
    ) {

        return progressRepository
                .findByGoalIdAndDate(
                        entity.getId(),
                        date
                )
                .orElseGet(() ->
                        GoalProgress.builder()
                                .goal(entity)
                                .date(date)
                                .completed(false)
                                .value(0.0)
                                .build()
                );
    }

    private void incrementProgress(
            UserGoal entity
    ) {

        int current =
                entity.getProgress() != null
                        ? entity.getProgress()
                        : 0;

        int target =
                entity.getTargetDays();

        if (current < target) {
            entity.setProgress(
                    current + 1
            );
        }
    }

    private void decrementProgress(
            UserGoal entity
    ) {

        int current =
                entity.getProgress() != null
                        ? entity.getProgress()
                        : 0;

        if (current > 0) {
            entity.setProgress(
                    current - 1
            );
        }

        /*
         * If the goal was completed and progress is reduced,
         * return it to ACTIVE.
         */
        if (entity.getStatus() == GoalStatus.COMPLETED) {

            entity.setStatus(
                    GoalStatus.ACTIVE
            );

            entity.setCompletedAt(null);
        }
    }

    private void completeGoalInternal(
            UserGoal entity
    ) {

        entity.setProgress(
                entity.getTargetDays()
        );

        entity.setStatus(
                GoalStatus.COMPLETED
        );

        entity.setCompletedAt(
                LocalDate.now()
        );

        log.info(
                "Goal {} completed",
                entity.getId()
        );
    }

    // =========================================================
    // PRIVATE: PERIOD LOGIC
    // =========================================================

    private boolean isCurrentPeriodCompleted(
            UserGoal entity,
            LocalDate date
    ) {

        /*
         * First check today's record.
         */
        boolean todayCompleted =
                progressRepository
                        .findByGoalIdAndDate(
                                entity.getId(),
                                date
                        )
                        .map(progress ->
                                Boolean.TRUE.equals(
                                        progress.getCompleted()
                                )
                        )
                        .orElse(false);

        if (todayCompleted) {
            return true;
        }

        return isCurrentPeriodCompletedExcludingToday(
                entity,
                date
        );
    }

    private boolean isCurrentPeriodCompletedExcludingToday(
            UserGoal entity,
            LocalDate today
    ) {

        List<LocalDate> completedDates =
                progressRepository
                        .findCompletedDatesByGoalId(
                                entity.getId()
                        );

        if (completedDates == null
                || completedDates.isEmpty()) {
            return false;
        }

        for (LocalDate completedDate : completedDates) {

            if (completedDate.equals(today)) {
                continue;
            }

            if (isSamePeriod(
                    entity.getFrequency(),
                    completedDate,
                    today
            )) {

                return true;
            }
        }

        return false;
    }

    private boolean isSamePeriod(
            Frequency frequency,
            LocalDate first,
            LocalDate second
    ) {

        return switch (frequency) {

            case DAILY ->
                    first.equals(second);

            case WEEKLY ->
                    first.getYear() == second.getYear()
                            && first.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
                            == second.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());

            case MONTHLY ->
                    first.getYear() == second.getYear()
                            && first.getMonth()
                            == second.getMonth();
        };
    }

    // =========================================================
    // PRIVATE: STREAK
    // =========================================================

    private int calculateStreak(
            UserGoal entity
    ) {

        List<LocalDate> completedDates =
                progressRepository
                        .findCompletedDatesByGoalId(
                                entity.getId()
                        );

        if (completedDates == null
                || completedDates.isEmpty()) {
            return 0;
        }

        /*
         * For DAILY goals:
         *
         * Today completed -> count backwards daily.
         *
         * Yesterday completed -> count backwards from yesterday.
         *
         * Otherwise -> 0.
         */
        if (entity.getFrequency()
                == Frequency.DAILY) {

            return calculateDailyStreak(
                    completedDates
            );
        }

        /*
         * For WEEKLY / MONTHLY:
         * Count consecutive completed periods.
         */
        return calculatePeriodStreak(
                entity,
                completedDates
        );
    }

    private int calculateDailyStreak(
            List<LocalDate> completedDates
    ) {

        LocalDate today =
                LocalDate.now();

        boolean todayCompleted =
                completedDates.contains(today);

        boolean yesterdayCompleted =
                completedDates.contains(
                        today.minusDays(1)
                );

        if (!todayCompleted
                && !yesterdayCompleted) {
            return 0;
        }

        LocalDate current =
                todayCompleted
                        ? today
                        : today.minusDays(1);

        int streak = 0;

        while (completedDates.contains(current)) {

            streak++;

            current =
                    current.minusDays(1);
        }

        return streak;
    }

    private int calculatePeriodStreak(
            UserGoal entity,
            List<LocalDate> completedDates
    ) {

        if (completedDates.isEmpty()) {
            return 0;
        }

        LocalDate today =
                LocalDate.now();

        int streak = 0;

        LocalDate periodDate = today;

        /*
         * If current period is not completed, allow streak
         * to start from the previous period.
         */
        if (!hasCompletedPeriod(
                entity,
                completedDates,
                periodDate
        )) {

            periodDate =
                    getPreviousPeriodStart(
                            entity.getFrequency(),
                            periodDate
                    );

            if (!hasCompletedPeriod(
                    entity,
                    completedDates,
                    periodDate
            )) {
                return 0;
            }
        }

        while (hasCompletedPeriod(
                entity,
                completedDates,
                periodDate
        )) {

            streak++;

            periodDate =
                    getPreviousPeriodStart(
                            entity.getFrequency(),
                            periodDate
                    );
        }

        return streak;
    }

    private boolean hasCompletedPeriod(
            UserGoal entity,
            List<LocalDate> completedDates,
            LocalDate periodDate
    ) {

        for (LocalDate completedDate : completedDates) {

            if (isSamePeriod(
                    entity.getFrequency(),
                    completedDate,
                    periodDate
            )) {
                return true;
            }
        }

        return false;
    }

    private LocalDate getPreviousPeriodStart(
            Frequency frequency,
            LocalDate date
    ) {

        return switch (frequency) {

            case DAILY ->
                    date.minusDays(1);

            case WEEKLY ->
                    date.minusWeeks(1);

            case MONTHLY ->
                    date.minusMonths(1);
        };
    }

    // =========================================================
    // PRIVATE: STATISTICS
    // =========================================================

    private double calculateTotalProgress(
            User user
    ) {

        List<UserGoal> goals =
                goalRepository.findByUser(user);

        if (goals == null
                || goals.isEmpty()) {
            return 0.0;
        }

        /*
         * Overall progress is based on completed target
         * periods across all goals.
         *
         * Example:
         *
         * Goal A = 5/10
         * Goal B = 8/20
         *
         * Overall = 13/30 = 43.33%
         */
        long totalTarget =
                goals.stream()
                        .filter(goal ->
                                goal.getStatus()
                                        != GoalStatus.ARCHIVED
                        )
                        .mapToLong(
                                UserGoal::getTargetDays
                        )
                        .sum();

        long totalCompleted =
                goals.stream()
                        .filter(goal ->
                                goal.getStatus()
                                        != GoalStatus.ARCHIVED
                        )
                        .mapToLong(goal ->
                                Math.min(
                                        goal.getProgress(),
                                        goal.getTargetDays()
                                )
                        )
                        .sum();

        if (totalTarget <= 0) {
            return 0.0;
        }

        return (totalCompleted * 100.0)
                / totalTarget;
    }

    // =========================================================
    // PRIVATE: VALIDATION
    // =========================================================

    private void validateGoalRequest(
            GoalRequest request
    ) {

        if (request == null) {
            throw new ValidationException(
                    "Goal request is required"
            );
        }

        if (request.getTitle() == null
                || request.getTitle()
                .trim()
                .isEmpty()) {

            throw new ValidationException(
                    "Title is required"
            );
        }

        if (request.getTitle()
                .trim()
                .length() > 200) {

            throw new ValidationException(
                    "Title must be less than 200 characters"
            );
        }

        if (request.getTargetDays() < 1) {

            throw new ValidationException(
                    "Target must be at least 1"
            );
        }

        /*
         * Keep your existing maximum.
         */
        if (request.getTargetDays() > 365) {

            throw new ValidationException(
                    "Target cannot exceed 365 periods"
            );
        }

        if (request.getDescription() != null
                && request.getDescription()
                .length() > 2000) {

            throw new ValidationException(
                    "Description cannot exceed 2000 characters"
            );
        }
    }

    private void validateStartDate(
            LocalDate startDate
    ) {

        /*
         * Allow today and future dates.
         *
         * If your product should allow backdated goals,
         * remove this validation.
         */
        if (startDate.isBefore(
                LocalDate.now()
        )) {

            throw new ValidationException(
                    "Start date cannot be in the past"
            );
        }
    }

    private void validateGoalDate(
            UserGoal entity,
            LocalDate today
    ) {

        if (today.isBefore(
                entity.getStartDate()
        )) {

            throw new ValidationException(
                    "Goal has not started yet"
            );
        }

        if (today.isAfter(
                entity.getTargetDate()
        )) {

            /*
             * Automatically mark expired.
             */
            entity.setStatus(
                    GoalStatus.EXPIRED
            );

            goalRepository.save(entity);

            throw new ValidationException(
                    "Goal has expired"
            );
        }
    }

    private void validateGoalStatusForUpdate(
            UserGoal entity
    ) {

        if (entity.getStatus()
                == GoalStatus.COMPLETED) {

            throw new ValidationException(
                    "Goal is already completed"
            );
        }

        if (entity.getStatus()
                == GoalStatus.CANCELLED) {

            throw new ValidationException(
                    "Goal has been cancelled"
            );
        }

        if (entity.getStatus()
                == GoalStatus.EXPIRED) {

            throw new ValidationException(
                    "Goal has expired"
            );
        }

        if (entity.getStatus()
                == GoalStatus.ARCHIVED) {

            throw new ValidationException(
                    "Goal has been archived"
            );
        }

        if (entity.getStatus()
                == GoalStatus.PAUSED) {

            throw new ValidationException(
                    "Goal is paused. Resume it before updating progress"
            );
        }
    }

    private void validateGoalCanBeEdited(
            UserGoal entity
    ) {

        if (entity.getStatus()
                == GoalStatus.COMPLETED) {

            throw new ValidationException(
                    "Completed goal cannot be edited"
            );
        }

        if (entity.getStatus()
                == GoalStatus.CANCELLED) {

            throw new ValidationException(
                    "Cancelled goal cannot be edited"
            );
        }

        if (entity.getStatus()
                == GoalStatus.EXPIRED) {

            throw new ValidationException(
                    "Expired goal cannot be edited"
            );
        }

        if (entity.getStatus()
                == GoalStatus.ARCHIVED) {

            throw new ValidationException(
                    "Archived goal cannot be edited"
            );
        }

        if (entity.getStatus()
                == GoalStatus.PAUSED) {

            throw new ValidationException(
                    "Resume the goal before editing it"
            );
        }
    }

    private void validateNoteContent(
            String content
    ) {

        if (content == null
                || content.trim().isEmpty()) {

            throw new ValidationException(
                    "Note content is required"
            );
        }

        if (content.trim().length() > 5000) {

            throw new ValidationException(
                    "Note cannot exceed 5000 characters"
            );
        }
    }

    // =========================================================
    // PRIVATE: OWNERSHIP
    // =========================================================

    private User getUserByEmail(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with email: "
                                        + email
                        )
                );
    }

    private UserGoal getGoalAndValidateOwnership(
            Long goalId,
            String email
    ) {

        UserGoal entity =
                goalRepository.findById(goalId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Goal not found with id: "
                                                + goalId
                                )
                        );

        /*
         * Return 404 instead of revealing that another
         * user's goal exists.
         */
        if (entity.getUser() == null
                || entity.getUser().getEmail() == null
                || !entity.getUser()
                .getEmail()
                .equalsIgnoreCase(email)) {

            throw new ResourceNotFoundException(
                    "Goal not found with id: "
                            + goalId
            );
        }

        return entity;
    }

    private void validateNoteOwnership(
            GoalNote note,
            String email
    ) {

        if (note.getGoal() == null
                || note.getGoal().getUser() == null
                || note.getGoal().getUser().getEmail() == null
                || !note.getGoal()
                .getUser()
                .getEmail()
                .equalsIgnoreCase(email)) {

            throw new SecurityException(
                    "Unauthorized to access this note"
            );
        }
    }

    // =========================================================
    // PRIVATE: UTILITY
    // =========================================================

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private String getDefaultUnit(
            Frequency frequency
    ) {

        return switch (frequency) {

            case DAILY ->
                    "days";

            case WEEKLY ->
                    "weeks";

            case MONTHLY ->
                    "months";
        };
    }
}