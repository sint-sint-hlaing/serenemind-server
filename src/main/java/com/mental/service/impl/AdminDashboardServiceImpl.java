package com.mental.service.impl;

import com.mental.dto.JournalDto;
import com.mental.dto.UserDto;
import com.mental.dto.mood.AuditLogDto;
import com.mental.dto.mood.DashboardStatsDto;
import com.mental.dto.mood.MoodDistributionDto;
import com.mental.model.entity.UserProfile;
import com.mental.repository.*;
import com.mental.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final MoodTrackingRepository moodRepository;
    private final JournalRepository journalRepository;
    private final MeditationRepository meditationRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    public DashboardStatsDto getDashboardOverview() {
        return new DashboardStatsDto(
                userRepository.count(),
                userRepository.countByLastLoginAfter(LocalDateTime.now().minusDays(3)),
                moodRepository.count(),
                journalRepository.count(),
                meditationRepository.count(),
                getMoodDistributionData()
        );
    }

    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> {
            // ထို user တစ်ယောက်ချင်းစီအတွက် profile ကို ရယူခြင်း
            UserProfile profile = user.getUserProfile();

            // DTO အဖြစ် ပြောင်းလဲပေးခြင်း
            return UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullname(profile != null ? profile.getFullname() : "N/A")
                    .avatarUrl(profile != null ? profile.getAvatar() : null)
                    .isActive(user.isActive())
                    .createdAt(LocalDateTime.from(user.getCreatedAt()))
                    .build();
        });
    }

    @Override
    public List<MoodDistributionDto> getMoodDistributionData() {
        long total = moodRepository.count();
        return moodRepository.getMoodDistribution().stream()
                .map(obj -> new MoodDistributionDto((String)obj[0], (long)obj[1], ((long)obj[1]*100.0/total)))
                .collect(Collectors.toList());
    }

    @Override
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        // 1. Database မှ AuditLog များကို ထုတ်ယူပြီး map() ကို အသုံးပြုသည်
        return auditLogRepository.findAll(pageable).map(log -> new AuditLogDto(
                log.getId(),
                log.getUsername(),
                log.getAction(),
                log.getTargetId(),
                log.getDetails(),
                log.getCreatedAt()
        ));
    }

    @Override
    public Page<JournalDto> getFlaggedJournals(Pageable pageable) {
        return journalRepository.findAllByFlaggedTrueOrderByCreatedAtDesc(pageable)
                .map(journal -> JournalDto.builder()
                        .id(journal.getId())
                        .title(journal.getTitle())
                        .content(journal.getContent())
                        .isFlagged(journal.isFlagged())
                        .flagReason(journal.getFlagReason())
                        .createdAt(LocalDateTime.from(journal.getCreatedAt()))
                        .userId(journal.getUser().getId()) // User ID ကိုပါ ထည့်ပေးခြင်း
                        .build());
    }

    @Override
    @Transactional
    public void resolveFlaggedJournal(Long journalId) {
        journalRepository.findById(journalId)
                .ifPresent(journal -> {
                    journal.setFlagged(false);
                    journal.setFlagReason("Resolved by Admin");
                    journalRepository.save(journal);
                });
    }
}