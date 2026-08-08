package com.mental.dto.chat;


import lombok.Data;

@Data
public class ChatRequest {
    private Long conversationId;
    private String message;
}
