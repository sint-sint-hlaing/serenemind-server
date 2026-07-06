package com.mental.dto.mood;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MoodEntryDto {
    @JsonIgnore
    private Long id;
    private String moodType;
    private int intensity;
    private String note;
    private String emotion;
    private int stressLevel;
    private String recommendation;
}
