package com.mental.service.admin;

import com.mental.dto.JournalDto;
import com.mental.dto.admin.JournalAdminDto;
import com.mental.dto.analysis.JournalTrendDto;

import java.util.List;
public interface AdminJournalService {

    void deleteJournal(Long id);


    List<JournalAdminDto> getJournals();

    List<JournalTrendDto> getJournalTrend();
}
