package com.mental.service;

import com.mental.dto.chat.ChatDto;
import com.mental.model.entity.ChatMessage;
import com.mental.model.entity.ChatSession;
import com.mental.repository.ChatMessageRepository;
import com.mental.repository.ChatSessionRepository;
import com.mental.repository.StarterPromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final StarterPromptRepository starterPromptRepository;
    private final ChatClient chatClient;

    @Transactional
    public ChatDto.Response processChat(ChatDto.Request request) {
        ChatSession session;

        // Session ရှိပြီးသား သို့မဟုတ် အသစ်ဖန်တီးခြင်း
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Session not found"));
        } else {
            session = new ChatSession();
            session.setUserId(request.getUserId());
            session.setTitle(generateTitleFromMessage(request.getMessage()));
            session = sessionRepository.save(session);
        }

        // 1. User Message သိမ်းခြင်း
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setSender(ChatMessage.SenderType.USER);
        userMsg.setContent(request.getMessage());
        messageRepository.save(userMsg);

        // 2. Groq/Gemini AI API ခေါ်ယူခြင်း
        String aiReply = chatClient.prompt()
                .user(request.getMessage())
                .call()
                .content();

        // 3. AI Response Message သိမ်းခြင်း
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSession(session);
        aiMsg.setSender(ChatMessage.SenderType.AI);
        aiMsg.setContent(aiReply);
        messageRepository.save(aiMsg);

        // 4. Session ၏ Last Message ကို Update လုပ်ခြင်း
        session.setLastMessage("SereneAI: " + aiReply);
        sessionRepository.save(session);

        return ChatDto.Response.builder()
                .sessionId(session.getId())
                .userMessage(request.getMessage())
                .aiResponse(aiReply)
                .timestamp(aiMsg.getCreatedAt())
                .quickReplies(List.of("Yes, please", "Not now", "Tell me more")) // UI Quick replies
                .build();
    }

    public List<ChatDto.SessionSummary> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(s -> ChatDto.SessionSummary.builder()
                        .id(s.getId())
                        .title(s.getTitle())
                        .lastMessage(s.getLastMessage())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .toList();
    }

    public List<ChatDto.MessageDetail> getSessionMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> ChatDto.MessageDetail.builder()
                        .id(m.getId())
                        .sender(m.getSender().name())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();
    }

    private String generateTitleFromMessage(String message) {
        if (message.length() > 25) {
            return message.substring(0, 25) + "...";
        }
        return message;
    }
}