package com.mental.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class MeditationResponse {


    private Long id;

    private String title;

    private String category;

    private Integer duration;

    private String audioUrl;

    private String description;

}
