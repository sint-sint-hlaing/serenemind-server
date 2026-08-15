package com.mental.dto.meditation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimerRequest {

    @NotNull(message = "Minutes are required")
    @Min(value = 1, message = "Minutes must be at least 1")
    @Max(value = 120, message = "Minutes cannot exceed 120")
    private Integer minutes;}
