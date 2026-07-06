package com.mental.dto;

import io.jsonwebtoken.ClaimsMutator;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for User information.
 * Combines core user data with profile details for admin viewing.
 */
@Builder
public record UserDto(
        Long id,
        String username,
        String email,
        String fullname,
        LocalDate birthday,
        String avatarUrl,
        boolean isActive,
        LocalDateTime createdAt
) {


}