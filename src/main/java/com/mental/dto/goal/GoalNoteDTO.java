package com.mental.dto.goal;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GoalNoteDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
