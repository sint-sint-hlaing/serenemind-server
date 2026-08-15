package com.mental.dto.goal;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GoalResponse {
    private Long id;
    private String title;
    private String description;
    private String frequency;
    private Integer targetDays;
    private String unit;
    private LocalDate startDate;
    private LocalDate targetDate;
    private LocalDate completedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private String icon;
    private String color;
    private Boolean silentMode;
    private Integer progress;
    private Integer totalDays;
    private String status;
    private List<ProgressHistoryDTO> history;
    private List<GoalNoteDTO> notes;
}
