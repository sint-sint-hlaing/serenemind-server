package com.mental.controller;

import com.mental.dto.chat.ChatRequest;
import com.mental.dto.chat.ConversationResponse;
import com.mental.dto.chat.MessageResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    @PostMapping("/send")
    public ResponseEntity<ConversationResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ChatRequest request) {
        ConversationResponse response = chatService.sendMessage(userPrincipal, request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getUserConversations(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ConversationResponse> conversations = chatService.getUserConversations(userPrincipal);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getConversationMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<MessageResponse> messages = chatService.getMessagesByConversationId(id, userPrincipal);
        return ResponseEntity.ok(messages);
    }


    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        chatService.deleteConversation(id, userPrincipal);
        return ResponseEntity.noContent().build();
    }
}