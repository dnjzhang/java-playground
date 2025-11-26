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
Two chat providers are exposed:
- Ollama: POST `/chat/ollama`
- OCI GenAI: POST `/chat/oci`

`OllamaChatbotService` and `OciChatbotService` live under `src/main/java/project/ollama/chat/service/` and are wired through `ChatController` (`src/main/java/project/ollama/chat/controller/ChatController.java`). Example call:
```java
String reply = ollamaChatbotService.chat("Summarize the latest release notes.");
```

Call the REST API with `curl`:
```bash
curl -X POST \
  -H "Content-Type: text/plain" \
  --data "Summarize the latest release notes." \
  http://localhost:8080/chat/ollama
```

Or use the helper script (`chat.sh`):
```bash
./chat.sh "Summarize the latest release notes."
```
Defaults target Ollama; use `CHAT_PROVIDER=oci` for OCI, and override host with `CHAT_HOST`:
```bash
CHAT_PROVIDER=oci CHAT_HOST=http://localhost:8080 ./chat.sh "Hi"
```

### OCI GenAI configuration (env or `application.yml`)
Set properties for the OCI model, for example (file-based auth):
```properties
spring.ai.oci.genai.authenticationType=file
spring.ai.oci.genai.file=/path/to/oci/config
spring.ai.oci.genai.cohere.chat.options.compartment=ocid1.compartment...
spring.ai.oci.genai.cohere.chat.options.servingMode=on-demand
spring.ai.oci.genai.cohere.chat.options.model=ocid1.generativeAIModel...
```
Ollama remains configured under `spring.ai.ollama.*`. Each endpoint uses its dedicated ChatClient bean.

## Build & Test
- Full build: `mvn clean package`
- Tests only: `mvn test` (requires Oracle connectivity or suitable stubs/containers)

## Troubleshooting
- Dependency resolution: ensure `spring-ai.version=1.0.0` is set in `pom.xml`.
- Model errors: verify the model is pulled in Ollama (`ollama run llama3` once to cache).  
