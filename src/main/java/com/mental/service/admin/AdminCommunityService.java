package com.mental.service.admin;

import com.mental.dto.Post.PostResponse;
import com.mental.dto.report.ReportDto;
import com.mental.dto.report.ReportSummaryDto;

import java.util.List;

public interface AdminCommunityService {
    // Community Management



    void deleteCommunityPost(Long id);

    List<ReportSummaryDto> getReports();

    List<PostResponse> getCommunityPosts();
}
