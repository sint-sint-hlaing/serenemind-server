package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeditationDashboardResponse {

    private MeditationResponse featured;

    private List<MeditationCategoryResponse> categories;

    private List<MeditationResponse> recommended;
}