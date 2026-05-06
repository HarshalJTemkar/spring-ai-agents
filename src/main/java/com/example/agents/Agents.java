package com.example.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Three specialised agents that share the same underlying LLM but have
 * different system prompts (= different roles).
 *
 *   1. Researcher - gathers facts using the webSearch tool
 *   2. Writer     - turns notes into a polished summary
 *   3. Reviewer   - proofreads and finalises the text
 */
@Component
public class Agents {

    private final ChatClient.Builder builder;

    public Agents(ChatClient.Builder builder) {
        this.builder = builder;
    }

    /** Researcher Agent: allowed to call the webSearch tool. */
    public String researcher(String topic) {
        return builder.build().prompt()
                .system("You are a Research Agent. Use the webSearch tool when you need facts. "
                      + "Return concise bullet-style notes only - no prose.")
                .user("Research this topic: " + topic)
                .toolNames("webSearch")
                .call()
                .content();
    }

    /** Writer Agent: turns research notes into an executive summary. */
    public String writer(String researchNotes) {
        return builder.build().prompt()
                .system("You are a Writer Agent. Convert the given notes into a clean, "
                      + "5-bullet executive summary suitable for a busy manager.")
                .user("Notes:\n" + researchNotes)
                .call()
                .content();
    }

    /** Reviewer Agent: polishes grammar and clarity, returns final text. */
    public String reviewer(String draft) {
        return builder.build().prompt()
                .system("You are a Reviewer Agent. Fix grammar, improve clarity, "
                      + "and ensure each bullet is self-contained. Return ONLY the final text.")
                .user(draft)
                .call()
                .content();
    }
}
