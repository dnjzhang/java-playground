# Ollama Chat Server (Oracle MCP + Spring AI)

## Overview
Spring Boot 3.2 service that exposes Oracle MCP tools and a minimal Gen AI chatbot powered by Spring AI’s `ChatClient`. The chatbot uses the configured Ollama model; database tools rely on an Oracle instance. Maven coordinates: `project.ollama.chat:ollama-chat-server`.

## Prerequisites
- Java 17 and Maven 3.9+
- Running Ollama server with your model pulled (default `llama3`).
- Oracle DB reachable at the connection string in `src/main/resources/application.yml` or overrides.

## Configure
Set environment variables for local runs:
```bash
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=llama3            # or another pulled model

```

The Ollama client is configured under `spring.ai.ollama` in `application.yml`; the default temperature is 0.1.

## Run
```bash
mvn spring-boot:run
```
The server starts on port 8080 with the MCP transport enabled.

## Using the chatbot (Jersey)
The `OllamaChatbotService` (`src/main/java/project/ollama/chat/service/OllamaChatbotService.java`) wraps `ChatClient` and is exposed via Jersey at `/chat` (`src/main/java/project/ollama/chat/controller/ChatController.java`). Example call:
```java
String reply = ollamaChatbotService.chat("Summarize the latest release notes.");
```

Call the REST API with `curl`:
```bash
curl -X POST \
  -H "Content-Type: text/plain" \
  --data "Summarize the latest release notes." \
  http://localhost:8080/chat
```

Or use the helper script (`chat.sh`):
```bash
./chat.sh "Summarize the latest release notes."
```
You can override the host with `CHAT_HOST`, e.g. `CHAT_HOST=http://localhost:8080 ./chat.sh "Hi"`.

## Build & Test
- Full build: `mvn clean package`
- Tests only: `mvn test` (requires Oracle connectivity or suitable stubs/containers)

## Troubleshooting
- Dependency resolution: ensure `spring-ai.version=1.0.0` is set in `pom.xml`.
- Model errors: verify the model is pulled in Ollama (`ollama run llama3` once to cache).  
