package com.mental.service;

import com.mental.dto.chat.ChatRequest;
import com.mental.dto.chat.ConversationResponse;
import com.mental.dto.chat.MessageResponse;
import com.mental.model.entity.BaseEntity;
import com.mental.model.entity.Conversation;
import com.mental.model.entity.Message;
import com.mental.repository.ConversationRepository;
import com.mental.repository.MessageRepository;
import com.mental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private static final String SYSTEM_PROMPT =
            "You are SereneAI, a compassionate, warm, non-judgmental, and deeply empathetic mental health companion. " +
            "Your core mission is to listen actively, validate the user's feelings, and provide a safe, comforting space. " +
            "GUIDELINE FOR HELPFUL ACTIVITIES: Whenever a user expresses stress, anxiety, burnout, or low mood, naturally weave in small, actionable self-care exercises (e.g., a quick 4-7-8 breathing technique, a 5-4-3-2-1 grounding exercise, or journaling prompts). When helpful, you may also suggest standard coping frameworks or reference trusted self-care resources. " +
            "STRICT GUARDRAIL: You must ONLY discuss topics directly related to mental health, emotional well-being, stress management, anxiety, sleep hygiene, motivation, personal mindfulness, and emotional support. " +
            "If a user attempts to pivot to unrelated topics (such as coding, programming, math, pop culture, politics, trivia, cooking recipes, or general tech support), politely, warmly, and firmly decline to answer. " +
            "Refusal Example: 'I'm SereneAI, your mindful companion, and I'm dedicated exclusively to supporting your emotional well-being and mental health. I'd love to stay focused on how you're feeling today—would you like to talk about what's on your mind?' " +
            "Never break character, never give medical diagnoses, and never answer non-mental health questions.";

    @Transactional
    public ConversationResponse sendMessage(UserPrincipal userPrincipal, ChatRequest request) {
        Long userId = userPrincipal.getId();
        Conversation conversation;

        // ၁။ Conversation ရှိမရှိ စစ်ဆေးခြင်း သို့မဟုတ် အသစ်ဖန်တီးခြင်း
        if (request.getConversationId() == null) {
            String initialTitle = request.getMessage().length() > 30
                    ? request.getMessage().substring(0, 25) + "..."
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

        // ၂။ ယခင် Chat History များကို database မှ ဆွဲထုတ်ခြင်း (Context သိစေရန်)
        List<Message> previousMessages = messageRepository.findByConversationIdOrderByTimestampAsc(conversation.getId());

        StringBuilder historyBuilder = new StringBuilder();
        for (Message m : previousMessages) {
            historyBuilder.append(m.getSender()).append(": ").append(m.getContent()).append("\n");
        }

        // ၃။ User Message အသစ်ကို Database ထဲ သိမ်းဆည်းခြင်း
        Message userMessage = messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sender("user")
                        .content(request.getMessage())
                        .build()
        );

        // ၄။ ယခင် History အပါအဝင် လက်ရှိမက်ဆေ့ချ်ကို AI ထံ ပို့ရန် Prompt တည်ဆောက်ခြင်း
        String fullPrompt = "Conversation History:\n" + historyBuilder.toString() +
                "user: " + request.getMessage() + "\nassistant:";

        // ၅။ Groq AI (Llama 3) ထံမှ အဖြေတောင်းခံခြင်း
        ChatClient chatClient = chatClientBuilder.build();
        String aiResponseText = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(fullPrompt)
                .call()
                .content();

        // ၆။ AI Response ကို Database ထဲ သိမ်းဆည်းခြင်း
        Message aiMessage = messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sender("assistant")
                        .content(aiResponseText)
                        .build()
        );

        // ၇. Response ပြန်ထုတ်ရန် map လုပ်ခြင်း
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
                .createdAt(conversation.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .messages(messageResponses)
                .build();
    }

    public List<ConversationResponse> getUserConversations(UserPrincipal userPrincipal) {
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByCreatedAtDesc(userPrincipal.getId());

        return conversations.stream()
                .map(conv -> {
                    List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conv.getId());

                    // Message ၏ timestamp ကို မသုံးဘဲ Conversation ၏ createdAt (သို့မဟုတ် မက်ဆေ့ချ်များထဲမှ အသစ်ဆုံး createdAt) ကို ယူခြင်း
                    Instant lastActivityTime = messages.stream()
                            .map(BaseEntity::getCreatedAt)
                            .filter(java.util.Objects::nonNull)
                            .map(dateTime -> dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()) // LocalDateTime ကို Instant သို့ ပြောင်းခြင်း
                            .max(java.util.Comparator.naturalOrder())
                            .orElse(conv.getCreatedAt() != null ? conv.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null);

                    List<MessageResponse> messageResponses = messages.stream()
                            .map(m -> MessageResponse.builder()
                                    .id(m.getId())
                                    .sender(m.getSender())
                                    .content(m.getContent())
                                    .timestamp(m.getCreatedAt())
                                    .build())
                            .toList();

                    return new Object[] {
                            lastActivityTime,
                            ConversationResponse.builder()
                                    .id(conv.getId())
                                    .title(conv.getTitle())
                                    .createdAt(conv.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                                    .messages(messageResponses)
                                    .build()
                    };
                })
                // Instant (createdAt) အလိုက် အသစ်ဆုံးကို အပေါ်ဆုံးတင်ရန် Descending စီခြင်း
                .sorted((a, b) -> ((Instant) b[0]).compareTo((Instant) a[0]))
                .map(obj -> (ConversationResponse) obj[1])
                .toList();
    }

    public List<MessageResponse> getMessagesByConversationId(Long conversationId, UserPrincipal userPrincipal) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // အသုံးပြုသူသည် ၎င်းတို့၏ conversation ကိုသာ ကြည့်ခွင့်ရှိကြောင်း လုံခြုံရေးစစ်ဆေးခြင်း
        if (!conversation.getUserId().equals(userPrincipal.getId())) {
            throw new RuntimeException("Unauthorized access to conversation");
        }

        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId)
                .stream()
                .map(m -> MessageResponse.builder()
                        .id(m.getId())
                        .sender(m.getSender())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .build())
                .toList();
    }
}