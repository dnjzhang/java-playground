package com.mcp.oracle.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

/**
 * Oracle database configuration class
 * 
 */
@Configuration
@ConfigurationProperties(prefix = "oracle")
@Data
public class OracleToolConfig {
    
    /**
     * Oracle database connection string
     */
    private String connectionString;
    
    /**
     * Oracle database username
     */
    private String username;
    
    /**
     * Oracle database password
     */
    private String password;
    
    public String getConnectionString() {
        return connectionString;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
} 