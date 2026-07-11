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

    /** Decrypted rich-text content returned to the client */
    private String content;

    /** Tags associated with the journal entry */
    private List<String> tags;

    /** True if user marked this as a favourite */
    private boolean favourite;

    /** True if this journal is private / locked */
    private boolean isPrivate;

    /** Optional attached photo URL */
    private String photoUrl;

    /** Truncated preview (first ~100 chars of plain text) for list cards */
    private String preview;

    private Instant createdAt;
    private Instant updatedAt;

    /** Inline analysis summary – populated when analysis exists */
    private JournalAnalysisResponse analysis;
}
