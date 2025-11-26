package project.ollama.chat.controller;

import project.ollama.chat.service.OllamaChatbotService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entrypoint for the Ollama-backed chatbot.
 */
@RestController
@RequestMapping(path = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
public class ChatController {

    private final OllamaChatbotService chatbotService;

    public ChatController(OllamaChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> chat(@RequestBody String message) {
        return ResponseEntity.ok(chatbotService.chat(message));
    }
}
