package com.mental.dto.analysis;

import lombok.Builder;

import java.time.LocalDate;
@Builder
public record JournalTrendDto(
        LocalDate date,

        Long totalJournals
) {
}
