package com.mental.mapper;

import com.mental.dto.goal.GoalNoteDTO;
import com.mental.dto.goal.GoalResponse;
import com.mental.dto.goal.ProgressHistoryDTO;
import com.mental.model.entity.GoalNote;
import com.mental.model.entity.GoalProgress;
import com.mental.repository.GoalNoteRepository;
import com.mental.repository.GoalProgressRepository;
import com.mental.repository.UserGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserGoalMapper {

    private final GoalProgressRepository progressRepository;
    private final GoalNoteRepository noteRepository;
    private final UserGoalRepository goalRepository;

    public GoalResponse toResponseDto(
            com.mental.model.entity.UserGoal entity) {

        if (entity == null) {
            return null;
        }

        GoalResponse dto = GoalResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())

                // Frequency
                .frequency(
                        entity.getFrequency() != null
                                ? entity.getFrequency().name()
                                : "DAILY"
                )

                // Goal target
                .targetDays(entity.getTargetDays())
                .unit(entity.getUnit())

                // Dates
                .startDate(entity.getStartDate())
                .targetDate(entity.getTargetDate())

                // UI
                .icon(entity.getIcon())
                .color(entity.getColor())
                .silentMode(
                        entity.getSilentMode() != null
                                ? entity.getSilentMode()
                                : false
                )

                // Progress
                .progress(
                        entity.getProgress() != null
                                ? entity.getProgress()
                                : 0
                )

                // Status
                .status(
                        entity.getStatus() != null
                                ? entity.getStatus().name()
                                : "ACTIVE"
                )
                .totalDays(
                        entity.getStartDate() != null && entity.getTargetDate() != null
                                ? (int) ChronoUnit.DAYS.between(
                                entity.getStartDate(),
                                entity.getTargetDate()
                        )
                                : 0

                )

                // Completion
                .completedAt(entity.getCompletedAt())

                // Audit
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // =========================================================
        // PROGRESS HISTORY
        // =========================================================

        dto.setHistory(buildWeeklyHistory(entity.getId()));

        // =========================================================
        // GOAL NOTES
        // =========================================================

        List<GoalNoteDTO> notes =
                noteRepository
                        .findByGoalIdOrderByCreatedAtDesc(entity.getId())
                        .stream()
                        .map(this::toNoteDto)
                        .collect(Collectors.toList());

        dto.setNotes(notes);

        // Progress တွက်ချက်ခြင်း
        Integer completedCount = progressRepository.countCompletedByGoalId(entity.getId());
        int totalDays = entity.getTargetDays() > 0 ? entity.getTargetDays() : 1;

        // ရာခိုင်နှုန်း (Percentage) နဲ့ ပြချင်ရင် -
        int progressPercentage = (completedCount != null) ? (completedCount * 100) / totalDays : 0;

        dto.setProgress(progressPercentage); // သို့မဟုတ် completedCount ကိုပဲ တိုက်ရိုက်ပြချင်ရင် completedCount လို့ ထည့်နိုင်ပါတယ်

        return dto;
    }

    // =============================================================
    // PROGRESS HISTORY MAPPER
    // =============================================================

    private List<ProgressHistoryDTO> buildWeeklyHistory(Long goalId) {
        // Fetch goal details to use its exact startDate and targetDate
        com.mental.model.entity.UserGoal goal = goalRepository.findById(goalId).orElse(null);

        LocalDate startDate = (goal != null && goal.getStartDate() != null)
                ? goal.getStartDate()
                : LocalDate.now();

        LocalDate endDate = (goal != null && goal.getTargetDate() != null)
                ? goal.getTargetDate()
                : startDate.plusDays(9); // fallback to span target days

        // Fetch completed dates and progress records for this date range
        List<LocalDate> completedDates = progressRepository.findCompletedDatesByGoalId(goalId);
        List<GoalProgress> progressList = progressRepository.findByGoalIdAndDateBetween(goalId, startDate, endDate);

        java.util.Map<LocalDate, GoalProgress> progressMap = progressList.stream()
                .collect(Collectors.toMap(GoalProgress::getDate, p -> p));

        List<ProgressHistoryDTO> history = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            boolean isCompleted = completedDates.contains(date);
            GoalProgress progress = progressMap.get(date);

            ProgressHistoryDTO dto = new ProgressHistoryDTO();
            dto.setDate(date);
            dto.setCompleted(isCompleted);
            dto.setNotes(progress != null ? progress.getNotes() : null);
            dto.setValue(isCompleted ? 1.0 : 0.0);

            history.add(dto);
        }
        return history;
    }

    // =============================================================
    // NOTE MAPPER
    // =============================================================

    public GoalNoteDTO toNoteDto(GoalNote note) {

        if (note == null) {
            return null;
        }

        GoalNoteDTO dto = new GoalNoteDTO();

        dto.setId(note.getId());
        dto.setContent(note.getContent());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());

        return dto;
    }
}