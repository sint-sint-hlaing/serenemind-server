// MeditationResponse.java
package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeditationResponse {
    private Long id;
    private String title;
    private String time;
    private String description;
    private String category;
    private String duration;
    private String imageUrl;
    private String audioUrl;
    private boolean favorite;
    private Integer durationSeconds;
    private Integer difficulty;
    private Boolean premium;
    private Long listenCount;
    private Long favoriteCount;
}