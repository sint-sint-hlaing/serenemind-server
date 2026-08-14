package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeditationTimeResponse {
    private String name;
    private String displayName;
    private String emoji;
    private String description;
}
