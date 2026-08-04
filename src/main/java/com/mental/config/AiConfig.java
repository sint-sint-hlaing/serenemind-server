package com.mental.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        String systemInstruction = "You are SereneAI, a compassionate, warm, and supportive mental health companion. " +
                "Your goal is to listen without judgment, offer practical coping strategies, and maintain a safe space.";

        return builder
                .defaultSystem(systemInstruction)
                .build();
    }
}