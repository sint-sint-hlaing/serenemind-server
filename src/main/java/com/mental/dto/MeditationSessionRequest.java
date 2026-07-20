package com.mental.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class MeditationSessionRequest {

    @NotNull
    private Long meditationId;
    private Integer durationMinutes;


    private boolean completed;

}