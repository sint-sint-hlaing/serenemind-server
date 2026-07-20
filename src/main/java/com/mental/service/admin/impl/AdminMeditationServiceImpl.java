package com.mental.service.admin.impl;

import com.mental.dto.admin.MeditationAdminDto;
import com.mental.dto.analysis.MeditationTrendDto;
import com.mental.dto.meditation.MeditationRequest;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.enums.MeditationCategory;
import com.mental.model.entity.enums.MeditationStatus;
import com.mental.repository.MeditationRepository;
import com.mental.repository.MeditationSessionRepository;
import com.mental.service.admin.AdminMeditationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AdminMeditationServiceImpl implements AdminMeditationService {
    private final MeditationRepository meditationRepository;
    private final MeditationSessionRepository meditationSessionRepository;


    @Override
    public List<MeditationAdminDto> getMeditations() {

        return meditationRepository.findAll()
                .stream()
                .map(m -> MeditationAdminDto.builder()

                        .id(m.getId())

                        .title(m.getTitle())

                        .description(m.getDescription())

                        .category(
                                m.getCategories().name()
                        )


                        .audioUrl(
                                m.getAudioUrl()
                        )

                        .imageUrl(
                                m.getImageUrl()
                        )

                        .active(
                                m.getStatus()
                                        == MeditationStatus.PUBLISHED
                        )


                        .build()
                )
                .toList();
    }

    @Override
    public MeditationAdminDto createMeditation(
            MeditationRequest request
    ) {

        Meditation meditation = new Meditation();


        meditation.setTitle(
                request.title()
        );


        meditation.setDescription(
                request.description()
        );


        meditation.setCategories(
                MeditationCategory.valueOf(
                        request.category().toUpperCase()
                )
        );


        meditation.setDuration(
                request.duration()
        );


        meditation.setAudioUrl(
                request.audioUrl()
        );


        meditation.setImageUrl(
                request.imageUrl()
        );


        meditation.setStatus(
                MeditationStatus.DRAFT
        );


        Meditation saved =
                meditationRepository.save(meditation);


        return MeditationAdminDto.builder()

                .id(saved.getId())

                .title(saved.getTitle())

                .description(saved.getDescription())

                .category(saved.getCategories().name())

                .duration(Integer.valueOf(saved.getDuration()))

                .audioUrl(saved.getAudioUrl())

                .imageUrl(saved.getImageUrl())

                .active(false)

                .createdAt(saved.getCreatedAtAsInstant())
                .build();
    }


    @Override
    public List<MeditationTrendDto> getMeditationTrend() {

        return meditationSessionRepository
                .findMeditationTrend()
                .stream()
                .map(row -> new MeditationTrendDto(
                        ((java.sql.Date) row[0])
                                .toLocalDate(),

                        ((Number) row[1])
                                .longValue()
                ))
                .toList();
    }

}