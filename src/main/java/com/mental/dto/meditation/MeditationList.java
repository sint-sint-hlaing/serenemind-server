package com.mental.dto.meditation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeditationList {
private Long id;

private String title;

private String thumbnail;

private String duration;

    }
