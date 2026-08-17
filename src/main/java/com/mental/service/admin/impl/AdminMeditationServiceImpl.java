package com.mental.service.admin.impl;

import com.mental.dto.AudioUploadResult;
import com.mental.dto.admin.MeditationAdminDto;
import com.mental.dto.analysis.MeditationTrendDto;
import com.mental.dto.meditation.MeditationRequest;
import com.mental.model.entity.Meditation;
import com.mental.model.entity.enums.MeditationCategory;
import com.mental.model.entity.enums.MeditationStatus;
import com.mental.model.entity.enums.MeditationTime;
import com.mental.repository.MeditationRepository;
import com.mental.repository.MeditationSessionRepository;
import com.mental.service.CloudinaryService;
import com.mental.service.admin.AdminMeditationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
                        .category(m.getCategory().name())
                        .audioUrl(m.getAudioUrl())
                        .imageUrl(m.getImageUrl())
                        .active(m.getStatus() == MeditationStatus.PUBLISHED)
                        .totalSessions(null)  // or m.getSessions().size()
                        .createdAt(m.getCreatedAtAsInstant())
                        .timeOfDay(m.getTimeOfDay() != null ? m.getTimeOfDay().name() : null)
                        .status(m.getStatus().name())
                        .featured(m.getFeatured())
                        .premium(m.getPremium())
                        .build()
                )
                .toList();
    }

    @Override
    public MeditationAdminDto createMeditation(MeditationRequest request) {
        log.info("Creating meditation: {}", request.title());

        // ✅ Audio file validation
        if (request.audioFile() == null || request.audioFile().isEmpty()) {
            log.error("Audio file is missing");
            throw new IllegalArgumentException("Audio file is required");
        }

        log.info("Audio file: {}, size: {} bytes",
                request.audioFile().getOriginalFilename(),
                request.audioFile().getSize());

        // ✅ Upload audio & Get actual duration from Cloudinary
        String audioUrl = null;
        int durationInSeconds = 0;

        try {
            AudioUploadResult audioResult = cloudinaryService.storeAudioFile(request.audioFile(), "audios");
            audioUrl = audioResult.url();
            durationInSeconds = audioResult.durationSeconds();
            log.info("Audio uploaded successfully: {}, duration: {}s", audioUrl, durationInSeconds);
        } catch (Exception e) {
            log.error("Failed to upload audio: {}", e.getMessage(), e);
            throw new RuntimeException("Audio upload failed: " + e.getMessage());
        }

        // ✅ Upload image (optional)
        String imageUrl = null;
        if (request.imageFile() != null && !request.imageFile().isEmpty()) {
            try {
                imageUrl = cloudinaryService.storeFile(request.imageFile(), "images");
                log.info("Image uploaded successfully: {}", imageUrl);
            } catch (Exception e) {
                log.warn("Image upload failed: {}", e.getMessage());
            }
        }

        // ✅ Build Meditation entity
        Meditation meditation = new Meditation();
        meditation.setTitle(request.title());
        meditation.setDescription(request.description());

        try {
            meditation.setCategory(MeditationCategory.valueOf(request.category().toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid category: {}", request.category());
            throw new IllegalArgumentException("Invalid category: " + request.category());
        }

        meditation.setDifficulty(request.difficulty());

        // ✅ MP3 ဖိုင်၏ အချိန်အမှန်အပေါ် မူတည်၍ Duration (Minutes) ကို Auto တွက်ပေးခြင်း
        // အနည်းဆုံး 1 မိနစ် သို့မဟုတ် Duration (စက္ကန့် / ၆၀)
        int calculatedMinutes = Math.max(1, (int) Math.round(durationInSeconds / 60.0));

        meditation.setDuration(String.valueOf(calculatedMinutes)); // "10"
        meditation.setDurationSeconds(durationInSeconds);          // 600

        meditation.setAudioUrl(audioUrl);
        meditation.setImageUrl(imageUrl);
        meditation.setStatus(MeditationStatus.PUBLISHED);

        // ✅ Set timeOfDay
        if (request.timeofDay() != null) {
            meditation.setTimeOfDay(request.timeofDay());
        } else {
            meditation.setTimeOfDay(MeditationTime.MORNING);
        }

        // ✅ Save
        Meditation saved = meditationRepository.save(meditation);
        log.info("Meditation saved with ID: {}", saved.getId());

        // ✅ Return DTO
        return MeditationAdminDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .category(saved.getCategory().name())
                .duration(Integer.valueOf(saved.getDuration()))
                .audioUrl(saved.getAudioUrl())
                .imageUrl(saved.getImageUrl())
                .active(false)
                .totalSessions(0L)
                .createdAt(saved.getCreatedAtAsInstant())
                .timeOfDay(saved.getTimeOfDay().name())
                .status(saved.getStatus().name())
                .featured(saved.getFeatured())
                .premium(saved.getPremium())
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
                    return new MeditationTrendDto(date, count);
                })
                .toList();
    }
}