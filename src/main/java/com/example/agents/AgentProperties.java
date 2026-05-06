package com.example.agents;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-agent retry / timeout configuration.
 *
 * Bind from application.properties using the prefix {@code agents}, e.g.
 *
 * <pre>
 * agents.researcher.max-attempts=4
 * agents.researcher.timeout=90s
 * agents.researcher.backoff-initial=2s
 * agents.researcher.backoff-multiplier=2.0
 * agents.researcher.backoff-max=30s
 * </pre>
 *
 * Keys: {@code researcher}, {@code writer}, {@code reviewer}.
 */
@ConfigurationProperties(prefix = "agents")
public class AgentProperties {

    /** Defaults applied when an agent has no explicit override. */
    private AgentSettings defaults = new AgentSettings();

    /** Per-agent overrides keyed by agent name. */
    private Map<String, AgentSettings> overrides = new HashMap<>();

    public AgentSettings forAgent(String name) {
        AgentSettings s = overrides.get(name);
        return s != null ? s : defaults;
    }

    public AgentSettings getDefaults() { return defaults; }
    public void setDefaults(AgentSettings defaults) { this.defaults = defaults; }

    public Map<String, AgentSettings> getOverrides() { return overrides; }
    public void setOverrides(Map<String, AgentSettings> overrides) { this.overrides = overrides; }

    // Convenience setters so flat keys like "agents.researcher.*" bind directly.
    public void setResearcher(AgentSettings s) { overrides.put("researcher", s); }
    public AgentSettings getResearcher() { return overrides.get("researcher"); }

    public void setWriter(AgentSettings s) { overrides.put("writer", s); }
    public AgentSettings getWriter() { return overrides.get("writer"); }

    public void setReviewer(AgentSettings s) { overrides.put("reviewer", s); }
    public AgentSettings getReviewer() { return overrides.get("reviewer"); }

    public static class AgentSettings {
        /** Total attempts including the first one. */
        private int maxAttempts = 3;
        /** Hard wall-clock timeout for a single attempt. */
        private Duration timeout = Duration.ofSeconds(60);
        /** Initial backoff delay between retries. */
        private Duration backoffInitial = Duration.ofSeconds(2);
        /** Cap on backoff delay. */
        private Duration backoffMax = Duration.ofSeconds(30);
        /** Exponential multiplier applied to the backoff. */
        private double backoffMultiplier = 2.0;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }

        public Duration getBackoffInitial() { return backoffInitial; }
        public void setBackoffInitial(Duration backoffInitial) { this.backoffInitial = backoffInitial; }

        public Duration getBackoffMax() { return backoffMax; }
        public void setBackoffMax(Duration backoffMax) { this.backoffMax = backoffMax; }

        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
    }
}
