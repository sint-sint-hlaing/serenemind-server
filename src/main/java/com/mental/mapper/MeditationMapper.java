// MeditationMapper.java
package com.mental.mapper;

import com.mental.dto.meditation.MeditationList;
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
                .category(meditation.getCategory() != null ?
                        meditation.getCategory().name() : null)
                .time(meditation.getTimeOfDay() != null ?
                        meditation.getTimeOfDay().name() : null)
                .duration(meditation.getDuration())
                .durationSeconds(meditation.getDurationSeconds())
                .audioUrl(meditation.getAudioUrl())
                .imageUrl(meditation.getImageUrl())
                .difficulty(meditation.getDifficulty())
                .premium(meditation.getPremium())
                .listenCount(meditation.getListenCount())
                .favoriteCount(meditation.getFavoriteCount())
                .favorite(false) // Default, will be set by service
                .build();
    }

    public MeditationList toListResponse(Meditation meditation) {
        if (meditation == null) {
            return null;
        }

        return MeditationList.builder()
                .id(meditation.getId())
                .title(meditation.getTitle())
                .thumbnail(meditation.getImageUrl())
                .duration(meditation.getDuration())
                .category(meditation.getCategory() != null ?
                        meditation.getCategory().name() : null)
                .time(meditation.getTimeOfDay() != null ?
                        meditation.getTimeOfDay().name() : null)
                .build();
    }
}