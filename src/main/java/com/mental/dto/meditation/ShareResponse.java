package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShareResponse {

    private String title;

    private String description;

    private String imageUrl;

    private String shareUrl;
}
