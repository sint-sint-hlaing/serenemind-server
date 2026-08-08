package com.mental.service.admin.impl;

import com.mental.dto.admin.MeditationAdminDto;
import com.mental.dto.analysis.MeditationTrendDto;
import com.mental.dto.meditation.MeditationRequest;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.enums.MeditationCategory;
import com.mental.model.entity.enums.MeditationStatus;
import com.mental.repository.MeditationRepository;
import com.mental.repository.MeditationSessionRepository;
import com.mental.service.CloudinaryService;
import com.mental.service.admin.AdminMeditationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AdminMeditationServiceImpl implements AdminMeditationService {
    private final MeditationRepository meditationRepository;
    private final MeditationSessionRepository meditationSessionRepository;
    private final CloudinaryService cloudinaryService;

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

        String audioUrl = null;
        if (request.audioFile() != null && !request.audioFile().isEmpty()) {
            audioUrl = cloudinaryService.storeFile(request.audioFile(), "audios");
        }

        String imageUrl = null;
        if (request.imageFile() != null && !request.imageFile().isEmpty()) {
            imageUrl = cloudinaryService.storeFile(request.imageFile(), "images");
        }

        Meditation meditation = new Meditation();

        meditation.setTitle(request.title());
        meditation.setDescription(request.description());

        meditation.setCategories(
                MeditationCategory.valueOf(
                        request.category().toUpperCase()
                )
        );

        meditation.setDifficulty(request.difficulty());

        meditation.setDuration(request.duration());

        Integer durationInMinutes = Integer.parseInt(request.duration());
        Integer durationInSeconds = durationInMinutes * 60;
        meditation.setDurationSeconds(durationInSeconds);

        meditation.setAudioUrl(audioUrl);
        meditation.setImageUrl(imageUrl);

        meditation.setStatus(MeditationStatus.DRAFT);

        Meditation saved = meditationRepository.save(meditation);

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
                .map(row -> {

                    LocalDate date = (LocalDate) row[0];

                    Long count = ((Number) row[1]).longValue();

                    return new MeditationTrendDto(
                            date,
                            count
                    );
                })
                .toList();
    }

}