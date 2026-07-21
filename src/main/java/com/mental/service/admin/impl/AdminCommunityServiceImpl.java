package com.mental.service.admin.impl;

import com.mental.dto.Post.PostResponse;
import com.mental.dto.report.ReportDto;
import com.mental.dto.report.ReportSummaryDto;
import com.mental.exception.ResourceNotFoundException;
import com.mental.mapper.PostMapper;
import com.mental.mapper.ReportMapper;
import com.mental.model.entity.Post;
import com.mental.repository.PostRepository;
import com.mental.repository.ReportRepository;
import com.mental.service.admin.AdminCommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommunityServiceImpl implements AdminCommunityService {

    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final PostMapper postMapper;
    private final ReportMapper reportMapper;

    @Override
    @Transactional
    public void deleteCommunityPost(Long id) {
        log.info("Deleting community post with id: {}", id);

        if (!postRepository.existsById(id)) {
            throw new ResourceNotFoundException("Post not found with id: " + id);
        }

        postRepository.deleteById(id);
        log.info("Successfully deleted community post with id: {}", id);
    }

    @Override
    public List<ReportSummaryDto> getReports() {
        log.info("Fetching community reports");

        long totalReports = reportRepository.count();
        long todayReports = reportRepository.countTodayReports();

        ReportSummaryDto summaryDto = ReportSummaryDto.builder()
                .id(1L)
                .reportType("COMMUNITY_REPORT")
                .total(totalReports)
                .todayCount(todayReports)
                .growthPercentage(0.0)
                .generatedAt(LocalDateTime.now())
                .build();

        // 👈 List တစ်ခုအဖြစ် ထည့်သွင်း၍ ပြန်ပေးခြင်း
        return List.of(summaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getCommunityPosts() {
        log.info("Fetching all community posts");

        return postRepository.findAll()
                .stream()
                .map(postMapper::toPostResponse)
                .toList();
    }
}