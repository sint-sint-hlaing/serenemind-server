package com.mental.service;

import com.mental.dto.chat.ChatRequest;
import com.mental.dto.chat.ConversationResponse;
import com.mental.dto.chat.MessageResponse;
import com.mental.model.entity.Conversation;
import com.mental.model.entity.Message;
import com.mental.repository.ConversationRepository;
import com.mental.repository.MessageRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private static final String SYSTEM_PROMPT =
            "You are SereneAI, a mindful, non-judgmental, and empathetic mental health companion. " +
                    "Always provide supportive, warm, and safe guidance. Keep responses structured and comforting.";

    @Transactional
    public ConversationResponse sendMessage(UserPrincipal userPrincipal, ChatRequest request) {
        Long userId = userPrincipal.getId();
        Conversation conversation;

        // ၁။ Conversation ရှိမရှိ စစ်ဆေးခြင်း သို့မဟုတ် အသစ်ဖန်တီးခြင်း
        if (request.getConversationId() == null) {
            String initialTitle = request.getMessage().length() > 30
                    ? request.getMessage().substring(0, 30) + "..."
                    : request.getMessage();

            conversation = conversationRepository.save(
                    Conversation.builder()
                            .userId(userId)
                            .title(initialTitle)
                            .build()
            );
        } else {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
        }

        // ၂။ User Message ကို သိမ်းဆည်းခြင်း
        Message userMessage = messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sender("user")
                        .content(request.getMessage())
                        .build()
        );

        // ၃. Groq AI (Llama 3) ထံမှ အဖြေတောင်းခံခြင်း
        ChatClient chatClient = chatClientBuilder.build();
        String aiResponseText = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(request.getMessage())
                .call()
                .content();

        // ၄။ AI Response ကို သိမ်းဆည်းခြင်း
        Message aiMessage = messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sender("assistant")
                        .content(aiResponseText)
                        .build()
        );

        // ၅. Response ပြန်ထုတ်ရန် map လုပ်ခြင်း
        List<MessageResponse> messageResponses = List.of(userMessage, aiMessage).stream()
                .map(m -> MessageResponse.builder()
                        .id(m.getId())
                        .sender(m.getSender())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .messages(messageResponses)
                .build();
    }

    public List<ConversationResponse> getUserConversations(UserPrincipal userPrincipal) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userPrincipal.getId())
                .stream()
                .map(conv -> ConversationResponse.builder()
                        .id(conv.getId())
                        .title(conv.getTitle())
                        .createdAt(conv.getCreatedAt())
                        .messages(
                                messageRepository.findByConversationIdOrderByTimestampAsc(conv.getId())
                                        .stream().map(m -> MessageResponse.builder()
                                                .id(m.getId())
                                                .sender(m.getSender())
                                                .content(m.getContent())
                                                .timestamp(m.getTimestamp())
                                                .build()).toList()
                        )
                        .build())
                .toList();
    }
}