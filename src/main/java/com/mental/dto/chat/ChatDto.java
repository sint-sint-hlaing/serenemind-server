package com.mental.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class ChatDto {

    @Data
    public static class Request {
        private Long userId;
        private Long sessionId; // null ဖြစ်ပါက Session အသစ်ဆောက်ပါမည်
        private String message;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Long sessionId;
        private String userMessage;
        private String aiResponse;
        private Instant timestamp;
        private List<String> quickReplies; // UI အောက်ခြေရှိ Suggested Options များ (Yes, please, Not now, etc.)
    }

    @Data
    @Builder
    public static class SessionSummary {
        private Long id;
        private String title;
        private String lastMessage;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class MessageDetail {
        private Long id;
        private String sender;
        private String content;
        private Instant createdAt;
    }
}
