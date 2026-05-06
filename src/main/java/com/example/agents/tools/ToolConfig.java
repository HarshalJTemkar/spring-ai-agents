package com.example.agents.tools;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Tools that agents are allowed to call.
 *
 * In Spring AI 1.0.0 GA, tools are exposed to the LLM via {@code @Tool}-annotated
 * methods registered through a {@link MethodToolCallbackProvider}. The agent
 * references them by the method name (or the {@code name} attribute on
 * {@code @Tool}) using {@code .toolNames("...")}.
 */
@Configuration
public class ToolConfig {

    /** Register every {@code @Tool} method on {@link WebTools} as a callable tool. */
    @Bean
    public ToolCallbackProvider agentTools(WebTools webTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(webTools)
                .build();
    }

    /** Holder for tool methods. Kept as a Spring bean so it can be injected/replaced. */
    @Component
    public static class WebTools {

        @Tool(name = "webSearch",
              description = "Search the web for a topic and return a short factual summary")
        public String webSearch(String query) {
            // TODO: integrate a real search API (Bing, Tavily, SerpAPI, etc.)
            return "Mock search results for '" + query + "': "
                    + "Tesla leads global EV market share; "
                    + "BYD dominates Asia with low-cost models; "
                    + "Rivian focuses on electric trucks and SUVs; "
                    + "Hyundai/Kia are gaining ground in Europe.";
        }
    }
}