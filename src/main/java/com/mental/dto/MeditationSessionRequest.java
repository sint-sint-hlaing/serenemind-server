package com.mental.dto;


import lombok.Data;


@Data
public class MeditationSessionRequest {


    private Long meditationId;
    private Integer durationMinutes;


    private boolean completed;

}