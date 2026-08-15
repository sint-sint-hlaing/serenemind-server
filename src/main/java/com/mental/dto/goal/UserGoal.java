// UserGoal.java
package com.mental.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGoal {
    private Long id;
    private String title;
    private String description;
    private String frequency;
    private Integer targetDays;
    private String unit;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String icon;
    private Boolean silentMode;
    private Integer progress;
    private Integer streak;
    private String note;
    private String status;
    private LocalDate completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // UI Specific Fields
    private List<ProgressHistoryDTO> history;
    private List<GoalNoteDTO> notes;
}

