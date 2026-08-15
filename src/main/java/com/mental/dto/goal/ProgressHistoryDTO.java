package com.mental.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor   // ← ဒါထည့်ပါ
@AllArgsConstructor
@Builder
public class ProgressHistoryDTO {
    private LocalDate date;
    private Boolean completed;
    private String notes;
    private Double value;
}
