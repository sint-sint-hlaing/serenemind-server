package com.mental.dto.goal;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ProgressHistoryDTO {
    private LocalDate date;
    private Boolean completed;
    private String notes;
    private Double value;
}
