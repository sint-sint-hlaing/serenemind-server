package com.mental.dto.goal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mental.model.entity.enums.Frequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRequest {

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be less than 200 characters")
        private String title;
        private Frequency frequency;
        @Size(max = 1000, message = "Description must be less than 1000 characters")
        private String description;

        @Min(value = 1, message = "Target days must be at least 1")
        @Max(value = 365, message = "Target days cannot exceed 365 days")
        private int targetDays;

        private String unit;
        private LocalDate startDate;
        private String icon;
        private String color;
        private Boolean silentMode;
        private Long userId;
}