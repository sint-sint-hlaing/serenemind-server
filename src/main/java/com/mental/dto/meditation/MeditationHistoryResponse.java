package com.mental.dto.meditation;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MeditationHistoryResponse {
    private Long id;
    private String title;
    private String category;
    private Integer duration;
    private boolean completed;
    private Integer progressPercentage;
    private LocalDateTime completedAt;
    private String imageUrl;
}
