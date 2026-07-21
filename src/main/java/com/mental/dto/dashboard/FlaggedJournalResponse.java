package com.mental.dto.dashboard;

import lombok.Builder;

import java.time.LocalDateTime;

public record FlaggedJournalResponse(

        Long id,

        String title,

        Long userId,

        String flagReason,

        boolean flagged,

        LocalDateTime createdAt

){}
