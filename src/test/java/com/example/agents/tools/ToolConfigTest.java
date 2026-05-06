package com.example.agents.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.example.agents.tools.ToolConfig.WebTools;

/**
 * Light unit tests for the tool layer. The real web search is mocked; we
 * only verify the tool is wired and returns deterministic mock content.
 */
class ToolConfigTest {

    @Test
    void webSearch_returnsDeterministicMockSummary() {
        WebTools tools = new WebTools();
        String result = tools.webSearch("EV market");

        assertThat(result)
                .contains("Mock search results for 'EV market'")
                .contains("Tesla")
                .contains("BYD");
    }

    @Test
    void agentTools_beanRegistersWebSearchCallback() {
        ToolConfig config = new ToolConfig();
        ToolCallbackProvider provider = config.agentTools(new WebTools());

        assertThat(provider).isNotNull();
        assertThat(provider.getToolCallbacks()).isNotEmpty();
        assertThat(provider.getToolCallbacks())
                .anyMatch(cb -> "webSearch".equals(cb.getToolDefinition().name()));
    }
}
