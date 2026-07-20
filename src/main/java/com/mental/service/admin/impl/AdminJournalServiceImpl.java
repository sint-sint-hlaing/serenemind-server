package com.mental.service.admin.impl;

import com.mental.dto.admin.JournalAdminDto;
import com.mental.dto.analysis.JournalTrendDto;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.Journal;
import com.mental.repository.JournalRepository;
import com.mental.service.admin.AdminJournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AdminJournalServiceImpl implements AdminJournalService {
    private final JournalRepository journalRepository;
    @Override
    public void deleteJournal(Long id) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found"));

        journalRepository.delete(journal);
    }

    @Override
    public List<JournalAdminDto> getJournals() {
        return journalRepository.findAllJournalForAdmin();
    }

    @Override
    public List<JournalTrendDto> getJournalTrend() {
        return journalRepository.findJournalTrend()
                .stream()
                .map(row -> new JournalTrendDto(
                        ((java.sql.Date) row[0]).toLocalDate(), // Date ကို LocalDate သို့ ပြောင်းခြင်း
                        ((Number) row[1]).longValue()           // Count ကို Long သို့ ပြောင်းခြင်း
                ))
                .toList();
    }
}
