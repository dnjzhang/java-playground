package com.mcp.oracle;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.mcp.oracle.service.OracleToolService;

/**
 * Oracle MCP Server Application
 * Main entry point for the Oracle MCP server application
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
public class OracleMcpServerApplication {

    /**
     * Main method to start the application
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OracleMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider oracleTools(OracleToolService oracleToolService) {
        return MethodToolCallbackProvider.builder().toolObjects(oracleToolService).build();
    }

}
