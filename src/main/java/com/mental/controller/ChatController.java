package com.mental.controller;

import com.mental.dto.chat.ChatDto;
import com.mental.model.entity.StarterPrompt;
import com.mental.repository.StarterPromptRepository;
import com.mental.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final StarterPromptRepository starterPromptRepository;

    // Chat Message ပို့ရန် API (Home & Chat Screen နှစ်ခုလုံးအတွက်)
    @PostMapping("/send")
    public ResponseEntity<ChatDto.Response> sendMessage(@RequestBody ChatDto.Request request) {
        return ResponseEntity.ok(chatService.processChat(request));
    }

    // Previous Conversations List ယူရန် API (UI Screen 1 အတွက်)
    @GetMapping("/sessions/user/{userId}")
    public ResponseEntity<List<ChatDto.SessionSummary>> getUserSessions(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getUserSessions(userId));
    }

    // Session တစ်ခုအတွင်းရှိ Chat History အကုန်ယူရန် API (UI Screen 2 အတွက်)
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatDto.MessageDetail>> getSessionMessages(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId));
    }

    // Start a conversation Prompts များယူရန် API (UI Screen 1 အတွက်)
    @GetMapping("/starter-prompts")
    public ResponseEntity<List<StarterPrompt>> getStarterPrompts() {
        return ResponseEntity.ok(starterPromptRepository.findAll());
    }
}