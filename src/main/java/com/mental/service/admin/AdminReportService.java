package com.mental.service.admin;

import com.mental.dto.report.ReportDto;
import com.mental.dto.report.ReportSummaryDto;

public interface AdminReportService {
    // Reports
    ReportSummaryDto userReport();

    ReportSummaryDto moodReport();

    ReportSummaryDto meditationReport();
}
