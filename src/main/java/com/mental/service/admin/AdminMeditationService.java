package com.mental.service.admin;

import com.mental.dto.admin.MeditationAdminDto;
import com.mental.dto.analysis.MeditationTrendDto;
import com.mental.dto.meditation.MeditationRequest;

import java.util.List;

public interface AdminMeditationService {
    List<MeditationAdminDto> getMeditations();

    MeditationAdminDto createMeditation(
            MeditationRequest request
    );

    List<MeditationTrendDto> getMeditationTrend();
}
