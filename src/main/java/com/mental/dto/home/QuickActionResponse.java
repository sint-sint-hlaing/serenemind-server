package com.mental.dto.home;

import lombok.Builder;

@Builder
public record QuickActionResponse(
        String title,
        String icon,
        String route
) {
}
