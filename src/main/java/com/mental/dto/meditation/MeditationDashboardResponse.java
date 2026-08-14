// MeditationDashboardResponse.java
package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeditationDashboardResponse {
    private List<MeditationCategoryResponse> categories;
    private List<MeditationTimeResponse> times;
    private List<MeditationResponse> featured;
    private List<MeditationResponse> recent;
    private List<MeditationResponse> popular;
    private MeditationStatistics statistics;

}