package project.ollama.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

/**
 * OCI GenAI-backed chatbot service using the configured Spring AI chat model.
 */
@Service
@ConditionalOnBean(name = "ociChatClient")
public class OciChatbotService {

    private final ChatClient chatClient;

    public OciChatbotService(@Qualifier("ociChatClient") ChatClient ociChatClient) {
        this.chatClient = ociChatClient;
    }

    /**
     * Send a user message to the OCI GenAI chat model and return the response text.
     *
     * @param userMessage user input to send to the model
     * @return model response content
     */
    public String chat(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message is required");
        }

        return chatClient.prompt()
                .system("You are an OCI GenAI assistant. Keep answers concise and actionable.")
                .user(userMessage.trim())
                .call()
                .content();
    }
}
