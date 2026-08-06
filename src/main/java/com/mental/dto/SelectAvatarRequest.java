package com.mental.dto;

import jakarta.validation.constraints.NotNull;

public record SelectAvatarRequest(
        @NotNull(message = "Avatar ID is required")
        Long avatarId
) {
}
