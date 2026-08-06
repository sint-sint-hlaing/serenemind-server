package com.mental.dto.chat;


import lombok.Data;

@Data
public class ChatRequest {
    private Long conversationId; // Null ဖြစ်ရင် အသစ်စမည်
    private String message;     // ဥပမာ - "I'm feeling stressed..."
}
