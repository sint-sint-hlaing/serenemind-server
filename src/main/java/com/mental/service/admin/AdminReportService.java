package com.mental.service.admin;

import com.mental.dto.report.ReportDto;

public interface AdminReportService {
    // Reports
    ReportDto userReport();

    ReportDto moodReport();

    ReportDto meditationReport();
}
