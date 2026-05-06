package com.example.agents;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: full Spring context loads with the per-agent configuration
 * and the OpenAI starter on the classpath. A dummy API key is supplied so
 * the auto-configuration can initialise without reaching the network.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-dummy-key",
        // Fast, deterministic per-agent settings for the smoke test.
        "agents.defaults.max-attempts=1",
        "agents.defaults.timeout=1s",
        "agents.defaults.backoff-initial=1ms",
        "agents.defaults.backoff-max=2ms",
        "agents.defaults.backoff-multiplier=1.0"
})
class AgenticAppTests {

    @Test
    void contextLoads() {
        // Empty body - failure here means wiring (beans, properties binding) is broken.
    }
}
