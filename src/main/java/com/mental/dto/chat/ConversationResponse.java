package com.mental.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConversationResponse {
    private Long id;
    private String title;
    private Instant createdAt;
    private List<MessageResponse> messages;
}