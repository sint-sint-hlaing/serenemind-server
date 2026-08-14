package com.mental.dto.goal;

import lombok.Data;
import java.time.LocalDate;

@Data
public class GoalProgressRequest {
    private Long goalId;
    private LocalDate date;
    private Boolean completed;
    private String notes;
    private Double value;
}
