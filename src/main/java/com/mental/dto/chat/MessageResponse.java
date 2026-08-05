package com.mental.dto.chat;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private String sender;
    private String content;
    private LocalDateTime timestamp;
}