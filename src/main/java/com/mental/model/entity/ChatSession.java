package com.mental.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
public class ChatSession extends BaseEntity {

    @Column(nullable = false)
    private Long userId; // Multi-user support

    private String title; // e.g. "Dealing with overthinking"

    private String lastMessage; // e.g. "SereneAI: Overthinking can be tiring..."

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();
}