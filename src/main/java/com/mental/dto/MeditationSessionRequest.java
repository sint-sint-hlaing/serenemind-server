package com.mental.dto;


import lombok.Data;


@Data
public class MeditationSessionRequest {


    private Long meditationId;

    private boolean completed;

}