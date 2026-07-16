package com.mental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for creating or updating a journal entry (text-only).
 *
 * Photo attachment is handled separately via:
 *   POST /api/journals/{id}/photo  (MultipartFile)
 *   DELETE /api/journals/{id}/photo
 */
@Getter
@Setter
public class JournalRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 50_000, message = "Content must not exceed 50,000 characters")
    private String content;

    /**
     * Tags chosen by user in the editor.
     * Max 10 tags, each max 50 characters, alphanumeric + hyphen/space only.
     * Prevents oversized-payload abuse and XSS via tag injection.
     */
    @Size(max = 10, message = "You may add up to 10 tags")
    private List<
        @NotBlank(message = "Tag must not be blank")
        @Size(max = 50, message = "Each tag must not exceed 50 characters")
        @Pattern(regexp = "^[\\w\\s\\-]+$", message = "Tags may only contain letters, numbers, spaces and hyphens")
        String> tags;

    /** Whether this journal entry is marked private / locked */
    private boolean isPrivate;

    /** Whether this journal is marked as favourite */
    private boolean favourite;
}
