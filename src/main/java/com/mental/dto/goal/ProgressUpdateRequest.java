package com.mental.dto.goal;

import lombok.Data;

@Data
public class ProgressUpdateRequest {
    private boolean completed; // true for Completed, false for Skipped
    private String note;       // Optional note text ("How did it go today?")
}
