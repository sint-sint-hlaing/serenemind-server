package com.mental.dto;

import lombok.Builder;

import java.time.LocalDateTime;
@Builder
public record JournalDto(
     Long id,
    String title,
    String content,
     boolean isFlagged,
    String flagReason,
     boolean isPrivate,
     LocalDateTime createdAt,
     Long userId

) {
}
