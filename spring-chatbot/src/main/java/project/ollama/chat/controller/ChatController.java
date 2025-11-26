package project.ollama.chat.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;
import project.ollama.chat.service.OllamaChatbotService;

/**
 * REST entrypoint for the Ollama-backed chatbot using Jersey (JAX-RS).
 */
@Component
@Path("/chat")
public class ChatController {

    private final OllamaChatbotService chatbotService;

    public ChatController(OllamaChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chat(String message) {
        return Response.ok(chatbotService.chat(message)).build();
    }
}
