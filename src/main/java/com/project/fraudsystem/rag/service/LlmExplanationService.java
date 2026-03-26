package com.project.fraudsystem.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LlmExplanationService {

    private final ChatClient chatClient;

    public LlmExplanationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateResponse(String prompt) {
        return chatClient.prompt(prompt)
                .call()
                .content();
    }
}