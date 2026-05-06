# Spring AI Multi-Agent Demo

A minimal **multi-agent** Spring Boot application using **Spring AI** + **OpenAI**.
It demonstrates a sequential pipeline of three specialised agents:

```
POST /agent/run
       │
       ▼
 Researcher Agent ──► webSearch tool
       │
       ▼
   Writer Agent (5-bullet summary)
       │
       ▼
  Reviewer Agent (proofreads)
       │
       ▼
   Final response (JSON)
```

## Requirements

- Java 17+
- Maven 3.9+
- An OpenAI API key

## Setup

1. Set your API key:

   **Windows (cmd):**
   ```cmd
   set OPENAI_API_KEY=sk-...
   ```

   **PowerShell:**
   ```powershell
   $env:OPENAI_API_KEY="sk-..."
   ```

2. Build:
   ```cmd
   mvn clean package
   ```

3. Run:
   ```cmd
   mvn spring-boot:run
   ```
   Or in **Eclipse**: right-click `AgenticApp.java` → `Run As` → `Spring Boot App`.

## Trigger the Agents

```cmd
curl -X POST http://localhost:8080/agent/run ^
     -H "Content-Type: application/json" ^
     -d "{\"goal\":\"Competitive analysis of EV cars\"}"
```

Sample response (truncated):
```json
{
  "goal": "Competitive analysis of EV cars",
  "researchNotes": "- Tesla leads global EV market...",
  "draft": "• Tesla holds the largest global EV market share...",
  "finalOutput": "• Tesla holds the largest global EV market share..."
}
```

## Project Layout

```
spring-ai-agents/
├── pom.xml
└── src/main/
    ├── java/com/example/agents/
    │   ├── AgenticApp.java              # Spring Boot entry point
    │   ├── Agents.java                  # Researcher / Writer / Reviewer
    │   ├── OrchestratorController.java  # /agent/run REST endpoint
    │   └── tools/ToolConfig.java        # webSearch tool (mock)
    └── resources/
        └── application.properties
```

## Customising

- **Add tools:** add more `@Tool`-annotated methods on `ToolConfig.WebTools`
  (or any bean you register with `MethodToolCallbackProvider`) and reference
  them by their `@Tool(name=...)` via `.toolNames("...")` in `Agents.java`.
- **Add agents:** create new methods in `Agents.java` with a different
  `system` prompt (role).
- **Change pattern:** swap the sequential pipeline in
  `OrchestratorController` for parallel calls, hierarchical delegation,
  or a debate/reflection loop.

## How It Works (Plain English)

1. The user POSTs a **goal**.
2. The **Researcher** asks the LLM, which decides to call `webSearch`,
   gets the results, and returns concise notes.
3. The **Writer** turns those notes into 5 polished bullets.
4. The **Reviewer** proofreads and returns the final answer.
5. All three steps share the same `ChatClient`/LLM but differ by
   **system prompt** — that is what makes them distinct *agents*.
