package com.mental.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class JournalResponse {

    private Long id;
    private String title;

    private String content;

    private List<String> tags;

    private boolean favourite;

    private String photoUrl;

    private String preview;

    private Instant createdAt;
    private Instant updatedAt;

    private JournalAnalysisResponse analysis;
}
