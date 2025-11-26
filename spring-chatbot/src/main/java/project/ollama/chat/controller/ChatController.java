package project.ollama.chat.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import project.ollama.chat.service.OciChatbotService;
import project.ollama.chat.service.OllamaChatbotService;

/**
 * REST entrypoint for the Ollama-backed chatbot using Jersey (JAX-RS).
 */
@Component
@Path("/chat")
public class ChatController {

    private final OllamaChatbotService chatbotService;
    private final OciChatbotService ociChatbotService;

    public ChatController(OllamaChatbotService chatbotService, ObjectProvider<OciChatbotService> ociChatbotService) {
        this.chatbotService = chatbotService;
        this.ociChatbotService = ociChatbotService.getIfAvailable();
    }

    @POST
    @Path("/ollama")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatOllama(String message) {
        return Response.ok(chatbotService.chat(message)).build();
    }

    @POST
    @Path("/oci")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatOci(String message) {
        if (ociChatbotService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("OCI chat is not configured").build();
        }
        return Response.ok(ociChatbotService.chat(message)).build();
    }
}
