package com.mental.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityResponse {
    private long totalJournals;
    private long goalsCompleted;
    private long totalPosts;
    private long completedMeditationsCount;
    private long favoriteMeditationsCount;
}
