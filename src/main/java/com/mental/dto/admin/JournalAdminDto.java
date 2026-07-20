package com.mental.dto.admin;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record JournalAdminDto(
        Long id,

        String title,

        String username,
        LocalDate createdDate

) {
}
