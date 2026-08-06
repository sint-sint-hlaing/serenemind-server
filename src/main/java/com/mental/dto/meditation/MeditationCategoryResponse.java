package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeditationCategoryResponse {

    private String name;

    private String emoji;
}