package com.mental.service.admin;

import com.mental.dto.Post.PostResponse;
import com.mental.dto.report.ReportDto;

import java.util.List;

public interface AdminCommunityService {
    // Community Management



    void deleteCommunityPost(Long id);

    List<ReportDto> getReports();

    List<PostResponse> getCommunityPosts();
}
