package com.mcp.oracle.service;

import java.util.ArrayList;
import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * MCP prompt provider for Java/Spring Boot code reviews.
 */
@Service
public class JavaCodeReviewPromptService {

    @McpPrompt(
            name = "java_code_review",
            title = "Back standard Java Review",
            description = "Review Java code and make sure code correctness and follow good coding hygiene.")
    public McpSchema.GetPromptResult javaCodeReviewPrompt(
            @McpArg(name = "file name") String fileName
    ) {

        String userContent = """
                You are an experience Java backend developer with strong Spring Boot & Oracle DB experience. You are asked to review the following file: 
                @%s
                Respond with:
                1) Make sure there is no logic error.
                2) Check for any issues with leaking resources. 
                3) Check if the code is well-formatted with proper tab/identation.
                4) If no issues are found, state "No issues found" and note residual risks.
                """.formatted(fileName);


        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage(Role.USER, new TextContent(userContent)));
        return new McpSchema.GetPromptResult(
                "Java Code Review Prompt", messages);
    }

    @McpComplete(prompt = "java_code_review")
    public List<String> completeFileName(String prefix) {
        // no suggestions for now
        return List.of();   // <- best option
    }
}
