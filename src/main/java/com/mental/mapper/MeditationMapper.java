package com.mental.mapper;

import com.mental.dto.meditation.MeditationResponse;
import com.mental.model.entity.Meditation;
import org.springframework.stereotype.Component;

@Component
public class MeditationMapper {

    public MeditationResponse toResponse(Meditation meditation) {
        if (meditation == null) {
            return null;
        }

        return MeditationResponse.builder()
                .id(meditation.getId())
                .title(meditation.getTitle())
                .description(meditation.getDescription())
                .category(meditation.getCategories().name())
                .duration(meditation.getDuration())
                .audioUrl(meditation.getAudioUrl())
                .imageUrl(meditation.getImageUrl())
                .build();
    }
}