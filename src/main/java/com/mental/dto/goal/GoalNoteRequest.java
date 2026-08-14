package com.mental.dto.goal;

import lombok.Data;

@Data
public class GoalNoteRequest {
    private Long goalId;
    private String content;
}
