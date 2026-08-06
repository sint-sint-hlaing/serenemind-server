package com.mental.dto.emotional;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResponse {

    private String emotion;

    private Integer percentage;

    private String message;

}