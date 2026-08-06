package com.mental.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record ProfileResponse(

        Long id,

        String fullname,

        LocalDate birthday,

        String profileImageUrl,

        Long avatarId,

        String avatarUrl,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}