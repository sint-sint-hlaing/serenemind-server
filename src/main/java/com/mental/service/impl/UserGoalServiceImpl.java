package com.mental.service.impl;

// ✅ DTO ကိုသာ import လုပ်ပါ
import com.mental.dto.goal.UserGoal;  // DTO - Record

// ✅ Entity ကို import မလုပ်ဘဲ Fully Qualified Name သုံးပါ
// import com.mental.model.entity.UserGoal;  // ❌ မသုံးပါနှင့်

import com.mental.dto.goal.GoalRequest;
import com.mental.dto.goal.GoalStatistics;
import com.mental.exception.ResourceNotFoundException;
import com.mental.exception.ValidationException;
import com.mental.mapper.UserGoalMapper;
import com.mental.model.entity.User;
import com.mental.model.entity.UserStreak;
import com.mental.model.entity.enums.GoalStatus;
import com.mental.repository.UserGoalRepository;
import com.mental.repository.UserRepository;
import com.mental.repository.UserStreakRepository;
import com.mental.service.UserGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserGoalServiceImpl implements UserGoalService {

    private final UserGoalRepository goalRepository;
    private final UserStreakRepository streakRepository;
    private final UserRepository userRepository;
    private final UserGoalMapper goalMapper;

    @Override
    public UserGoal createGoal(String email, GoalRequest request) {
        log.info("Creating goal for user: {}", email);

        validateGoalRequest(request);

        User user = getUserByEmail(email);
        LocalDate targetDate = LocalDate.now().plusDays(request.getTargetDays());

        // ✅ Entity ကို Fully Qualified Name ဖြင့်သုံးပါ
        com.mental.model.entity.UserGoal entity = com.mental.model.entity.UserGoal.builder()
                .user(user)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .targetDays(request.getTargetDays())
                .targetDate(targetDate)
                .progress(0)
                .status(GoalStatus.ACTIVE)
                .build();

        // ✅ Repository က Entity ကိုသာ save လုပ်နိုင်သည်
        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Goal created successfully with id: {}", saved.getId());

        // ✅ Entity -> DTO သို့ပြောင်းပြီး return လုပ်ပါ
        return goalMapper.toDto(saved);
    }

    @Override
    public UserGoal updateProgress(Long id, String email) {
        log.info("Updating progress for goal: {} by user: {}", id, email);

        // ✅ Entity ကို Fully Qualified Name ဖြင့်သုံးပါ
        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);

        validateGoalStatusForUpdate(entity);

        if (!canUpdateProgress(entity)) {
            log.debug("Progress already updated today for goal: {}", id);
            return goalMapper.toDto(entity);
        }

        incrementProgress(entity);
        updateStreak(entity.getUser());

        if (entity.getProgress() >= entity.getTargetDays()) {
            completeGoalInternal(entity);
        }

        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Progress updated for goal: {}, new progress: {}/{}",
                id, entity.getProgress(), entity.getTargetDays());

        return goalMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGoal> getUserGoals(String email) {
        log.debug("Fetching all goals for user: {}", email);

        User user = getUserByEmail(email);

        // ✅ Repository က Entity list ကိုပြန်ပေးသည်
        List<com.mental.model.entity.UserGoal> entities = goalRepository.findByUser(user);

        // ✅ Entity list ကို DTO list သို့ပြောင်းပါ
        return entities.stream()
                .map(goalMapper::toDto)
                .toList();
    }

    @Override
    public UserGoal completeGoal(Long id, String email) {
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

        return goalMapper.toDto(saved);
    }

    @Override
    public UserGoal pauseGoal(Long id, String email) {
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

        return goalMapper.toDto(saved);
    }

    @Override
    public UserGoal resumeGoal(Long id, String email) {
        log.info("Resuming goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);

        if (entity.getStatus() != GoalStatus.PAUSED) {
            throw new ValidationException("Goal is not paused");
        }

        entity.setStatus(GoalStatus.ACTIVE);
        com.mental.model.entity.UserGoal saved = goalRepository.save(entity);
        log.info("Goal {} resumed successfully", id);

        return goalMapper.toDto(saved);
    }

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

    @Override
    public void hardDeleteGoal(Long id, String email) {
        log.info("Hard deleting goal: {} by user: {}", id, email);

        com.mental.model.entity.UserGoal entity = getGoalAndValidateOwnership(id, email);
        goalRepository.delete(entity);

        log.info("Goal {} hard deleted successfully", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGoal> getActiveGoals(String email) {
        log.debug("Fetching active goals for user: {}", email);

        User user = getUserByEmail(email);

        return goalRepository.findByUserAndStatusIn(user,
                        List.of(GoalStatus.ACTIVE, GoalStatus.PAUSED))
                .stream()
                .map(goalMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGoal> getCompletedGoals(String email) {
        log.debug("Fetching completed goals for user: {}", email);

        User user = getUserByEmail(email);

        return goalRepository.findByUserAndStatus(user, GoalStatus.COMPLETED)
                .stream()
                .map(goalMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGoal> getGoalsByStatus(String email, GoalStatus status) {
        log.debug("Fetching goals for user: {} with status: {}", email, status);

        User user = getUserByEmail(email);

        return goalRepository.findByUserAndStatus(user, status)
                .stream()
                .map(goalMapper::toDto)
                .toList();
    }

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

    @Transactional(readOnly = true)
    public List<UserGoal> getGoalsForDashboard(String email) {
        log.debug("Fetching dashboard goals for user: {}", email);

        User user = getUserByEmail(email);

        return goalRepository.findByUserAndStatusOrderByCreatedAtDesc(user, GoalStatus.ACTIVE)
                .stream()
                .limit(5)
                .map(goalMapper::toDto)
                .toList();
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    // ✅ Entity ကို Fully Qualified Name ဖြင့်သုံးပါ
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

    private void updateStreak(User user) {
        UserStreak streak = streakRepository.findByUser(user)
                .orElseGet(() -> createNewStreak(user));

        LocalDate today = LocalDate.now();

        if (streak.getLastCompleted() != null && streak.getLastCompleted().equals(today)) {
            log.debug("Streak already updated for user: {}", user.getEmail());
            return;
        }

        if (streak.getLastCompleted() != null &&
                streak.getLastCompleted().equals(today.minusDays(1))) {
            streak.setStreakCount(streak.getStreakCount() + 1);
            log.debug("Streak incremented for user: {}, new count: {}",
                    user.getEmail(), streak.getStreakCount());
        } else {
            streak.setStreakCount(1);
            log.debug("New streak started for user: {}", user.getEmail());
        }

        streak.setLastCompleted(today);
        streakRepository.save(streak);
    }

    private UserStreak createNewStreak(User user) {
        UserStreak streak = new UserStreak();
        streak.setUser(user);
        streak.setStreakCount(0);
        return streakRepository.save(streak);
    }
}