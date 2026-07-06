package com.mental.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MeditationHistoryResponse {


    private String title;

    private Integer duration;

    private boolean completed;

    private Instant completedAt;

}