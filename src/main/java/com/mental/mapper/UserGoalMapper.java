// UserGoalMapper.java
package com.mental.mapper;

import com.mental.dto.goal.GoalNoteDTO;
import com.mental.dto.goal.GoalResponse;
import com.mental.dto.goal.ProgressHistoryDTO;
import com.mental.dto.goal.UserGoal;
import com.mental.model.entity.GoalNote;
import com.mental.model.entity.GoalProgress;
import com.mental.repository.GoalNoteRepository;
import com.mental.repository.GoalProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserGoalMapper {

    private final GoalProgressRepository progressRepository;
    private final GoalNoteRepository noteRepository;

    public GoalResponse toResponseDto(com.mental.model.entity.UserGoal
     entity) {
        if (entity == null) return null;

        GoalResponse dto = GoalResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .frequency(entity.getFrequency() != null ? entity.getFrequency().name() : "DAILY")
                .targetDays(entity.getTargetDays())
                .unit(entity.getUnit())
                .startDate(entity.getStartDate())
                .targetDate(entity.getTargetDate())
                .icon(entity.getIcon())
                .silentMode(entity.getSilentMode())
                .progress(entity.getProgress())
                .streak(entity.getStreak())
                .status(entity.getStatus().name())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Get progress history (last 7 days)
        List<GoalProgress> progressList = progressRepository.findByGoalIdOrderByDateDesc(entity.getId());
        List<ProgressHistoryDTO> history = progressList.stream()
                .limit(7)
                .map(p -> {
                    ProgressHistoryDTO historyDto = new ProgressHistoryDTO();
                    historyDto.setDate(p.getDate());
                    historyDto.setCompleted(p.getCompleted());
                    historyDto.setNotes(p.getNotes());
                    historyDto.setValue(p.getValue());
                    return historyDto;
                })
                .collect(Collectors.toList());
        dto.setHistory(history);

        // Get notes
        List<GoalNoteDTO> notes = noteRepository.findByGoalIdOrderByCreatedAtDesc(entity.getId())
                .stream()
                .map(this::toNoteDto)
                .collect(Collectors.toList());
        dto.setNotes(notes);

        return dto;
    }

    public GoalNoteDTO toNoteDto(GoalNote note) {
        if (note == null) return null;

        GoalNoteDTO dto = new GoalNoteDTO();
        dto.setId(note.getId());
        dto.setContent(note.getContent());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());
        return dto;
    }
}