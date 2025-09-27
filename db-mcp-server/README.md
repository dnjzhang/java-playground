# Oracle MCP Server

A Model Context Protocol (MCP) server implementation for Oracle database operations, built on top of `spring-ai-mcp-server-webmvc-spring-boot-starter`. This service provides a set of tools for interacting with Oracle databases through MCP, utilizing Server-Sent Events (SSE) for real-time communication.

## Features

- List all tables in the Oracle database
- Describe table structure (columns, data types, constraints)
- Execute SQL statements (SELECT, INSERT, UPDATE, DELETE)
- Secure database connection management
- Real-time communication via SSE
- Built on spring-ai-mcp-server-webmvc-spring-boot-starter

## Technology Stack

### Core Dependencies
- Spring Boot 3.2.0
- spring-ai-mcp-server-webmvc-spring-boot-starter
- Oracle JDBC Driver
- Model Context Protocol (MCP) Framework
- Lombok

### Development Tools
- Java 17
- Maven 3.6+
- Spring Boot Test Framework
- JUnit 5

## Getting Started

### Prerequisites

- JDK 17
- Maven 3.6+
- Oracle Database instance
- MCP Server environment
- Cursor IDE

### Configuration

#### 1. Database Configuration
Create or modify `application.properties` or `application.yml` with the following settings:

```yaml
oracle:
  connectionString: jdbc:oracle:thin:@//your-oracle-host:1521/your-service-name
  username: your-username
  password: your-password
```

#### 2. Cursor MCP Configuration
In your Cursor IDE, configure the MCP server in the settings:

```json
{
  "mcpServers": {
    "oracle-mcp-server": {
      "url": "http://{server-ip}:{server-port}/sse",
      "enabled": true
    }
  }
}
```

This configuration:
- Sets the MCP server name as "oracle-mcp-server"
- Configures the server URL to "http://{server-ip}:{server-port}/sse"
  - Replace {server-ip} with your server's IP address
  - Replace {server-port} with your server's port number
- Enables the MCP server for use in Cursor

### Building the Project

```bash
mvn clean install
```

### Running the Application

```bash
mvn spring-boot:run
```

## API Tools

### 1. List Tables Tool
- Name: `list_tables`
- Description: Get a list of all tables in Oracle database
- Usage: No parameters required
- Returns: Newline-separated list of table names

### 2. Describe Table Tool
- Name: `describe_table`
- Description: Get structure information of specified table
- Parameter: `tableName` - Name of the table to describe
- Returns: CSV format of table structure including:
  - Column names
  - Data types
  - Nullable status
  - Data length
  - Primary key information

### 3. Execute SQL Tool
- Name: `execute_sql`
- Description: Execute Oracle SQL statement
- Parameter: `sql` - SQL statement to execute
- Returns:
  - For SELECT: CSV format of query results
  - For INSERT/UPDATE/DELETE: Number of affected rows

## Implementation Details

### Architecture

```
com.mcp.oracle/
├── config/
│   └── OracleConfig.java         # Database configuration
├── service/
│   └── OracleService.java        # Core service implementation
└── OracleApplication.java        # Application entry point
```

### Key Components

1. **OracleConfig**
   - Handles database connection configuration
   - Uses Spring's @ConfigurationProperties
   - Secure password management

2. **OracleService**
   - Implements MCP tools for database operations
   - Manages database connections
   - Handles SQL execution and result formatting
   - Implements error handling and logging

3. **Connection Management**
   - Uses Oracle's connection pooling
   - Implements auto-closing of resources
   - Handles connection errors gracefully

### Security Considerations

- Password encryption in configuration
- Connection pool management
- SQL injection prevention
- Error message sanitization

## Testing

The project includes comprehensive unit tests:

```bash
mvn test
```

Test coverage includes:
- Database connection
- Table listing
- Table structure description
- SQL execution
- Error handling

## Error Handling

The service implements robust error handling:
- Connection failures
- Invalid SQL statements
- Missing tables/columns
- Permission issues

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please create an issue in the repository. 