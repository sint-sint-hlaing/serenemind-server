package com.mental.dto;


import lombok.Builder;

/**
 * DTO for creating or updating a User Goal.
 */
@Builder
public record GoalRequest(
        String title,
        String description,
        int targetDays
) {}
