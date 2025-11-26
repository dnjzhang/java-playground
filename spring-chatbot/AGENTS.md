# Repository Guidelines

## Project Structure & Module Organization
- Spring Boot 3.2 (Java 17). Entry point: `src/main/java/com/mcp/oracle/OracleMcpServerApplication.java`.
- Services: `src/main/java/com/mcp/oracle/service/` (Oracle tools, release-notes resources, MCP prompts, Ollama chatbot). HTTP entrypoint for chat lives in `src/main/java/com/mcp/oracle/controller/ChatController.java`.
- Config binding: `src/main/java/com/mcp/oracle/config/`. Core config in `src/main/resources/application.yml`; prompts in `src/main/resources/prompts/`; release notes in `src/main/resources/release-notes/`.
- Tests: `src/test/java/com/mcp/oracle/` with the `test` profile; ensure Oracle connectivity or stubs.

## Build, Test, and Development Commands
- `mvn clean package` — compile and run all tests; builds the bootable JAR.
- `mvn test` — JUnit 5/Spring Boot tests (needs Oracle creds/connection).
- `ORACLE_USERNAME=... ORACLE_PASSWORD=... mvn spring-boot:run` — start locally on port 8080 with MCP transport enabled.
- `mvn -DskipTests package` — build when DB access is unavailable.

## Coding Style & Naming Conventions
- 4-space indentation; PascalCase for classes, camelCase for members/methods; consistent SLF4J logging.
- Prefer constructor injection and Spring stereotypes (`@Service`, `@ConfigurationProperties`). Tool/prompt/resource names stay descriptive and snake_case.
- Avoid wildcard imports; keep SQL/JSON building readable; reuse helpers in `OracleToolService` for escaping/formatting.

## Testing Guidelines
- JUnit 5 via `spring-boot-starter-test`; tests named `*Test` under mirrored packages.
- Use `@ActiveProfiles("test")`; seed minimal Oracle data or mock where practical.
- Cover success/failure paths (invalid SQL, empty results, null inputs for chat). Keep logs free of sensitive values.

## Commit & Pull Request Guidelines
- Commit messages: short, imperative, focused (Conventional Commits not required). Example: `Add chat controller for Ollama`.
- PRs: describe the change, note config/DB impacts, list tests run (`mvn test`/manual), attach logs or screenshots if relevant, and link issues/tickets when present.

## Security & Configuration Tips
- Do not commit secrets; supply `ORACLE_USERNAME`, `ORACLE_PASSWORD`, `OLLAMA_BASE_URL`, and `OLLAMA_MODEL` via env vars (see `application.yml`).
- Validate SQL inputs, prefer prepared statements for new DB logic, and avoid logging sensitive fields. Ensure Ollama is secured if accessed remotely.
