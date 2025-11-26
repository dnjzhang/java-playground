package project.ollama.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Minimal Gen AI chatbot service backed by the configured Spring AI chat model (Ollama).
 */
@Service
public class OllamaChatbotService {

    private final ChatClient chatClient;

    public OllamaChatbotService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Send a user message to the configured chat model and return the response text.
     *
     * @param userMessage user input to send to the model
     * @return model response content
     */
    public String chat(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message is required");
        }

        return chatClient.prompt()
                .system("You are a helpful knowledge assistant. Keep answers concise and actionable.")
                .user(userMessage.trim())
                .call()
                .content();
    }
}
