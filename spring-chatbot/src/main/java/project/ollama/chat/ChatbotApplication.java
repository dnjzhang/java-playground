package project.ollama.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Oracle MCP Server Application
 * Main entry point for the Oracle MCP server application
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
public class ChatbotApplication {

    /**
     * Main method to start the application
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }


}
