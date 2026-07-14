package com.mental.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Response DTO returned after a photo is uploaded or deleted
 * from a journal entry.
 *
 * Endpoint: POST /api/journals/{id}/photo
 *           DELETE /api/journals/{id}/photo
 */
@Getter
@Setter
public class JournalPhotoResponse {

    private Long journalId;

    /**
     * The publicly accessible URL of the stored photo.
     * null when photo has been deleted.
     */
    private String photoUrl;

    /** Human-readable status message */
    private String message;
}
