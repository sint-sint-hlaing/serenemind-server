package com.mental.dto;

import lombok.Builder;

@Builder
public record ImageResponse(
        String imageUrl

) {
}
