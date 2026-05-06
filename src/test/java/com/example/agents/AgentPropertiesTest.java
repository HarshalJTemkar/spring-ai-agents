package com.example.agents;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import com.example.agents.AgentProperties.AgentSettings;

/**
 * Verifies that {@link AgentProperties} correctly binds per-agent overrides
 * and falls back to defaults when an agent is not declared.
 */
class AgentPropertiesTest {

    @Test
    void bindsDefaults_andPerAgentOverrides() {
        Map<String, Object> map = new HashMap<>();
        map.put("agents.defaults.max-attempts", 3);
        map.put("agents.defaults.timeout", "60s");
        map.put("agents.defaults.backoff-initial", "2s");
        map.put("agents.defaults.backoff-max", "30s");
        map.put("agents.defaults.backoff-multiplier", 2.0);

        map.put("agents.researcher.max-attempts", 4);
        map.put("agents.researcher.timeout", "90s");

        map.put("agents.writer.max-attempts", 2);
        map.put("agents.writer.timeout", "45s");

        AgentProperties props = bind(map);

        assertThat(props.getDefaults().getMaxAttempts()).isEqualTo(3);
        assertThat(props.getDefaults().getTimeout()).isEqualTo(Duration.ofSeconds(60));

        AgentSettings researcher = props.forAgent("researcher");
        assertThat(researcher.getMaxAttempts()).isEqualTo(4);
        assertThat(researcher.getTimeout()).isEqualTo(Duration.ofSeconds(90));

        AgentSettings writer = props.forAgent("writer");
        assertThat(writer.getMaxAttempts()).isEqualTo(2);
        assertThat(writer.getTimeout()).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void unknownAgent_fallsBackToDefaults() {
        Map<String, Object> map = new HashMap<>();
        map.put("agents.defaults.max-attempts", 7);
        map.put("agents.defaults.timeout", "11s");

        AgentProperties props = bind(map);

        AgentSettings unknown = props.forAgent("does-not-exist");
        assertThat(unknown).isSameAs(props.getDefaults());
        assertThat(unknown.getMaxAttempts()).isEqualTo(7);
        assertThat(unknown.getTimeout()).isEqualTo(Duration.ofSeconds(11));
    }

    private static AgentProperties bind(Map<String, Object> map) {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
        return new Binder(source)
                .bind("agents", AgentProperties.class)
                .orElseGet(AgentProperties::new);
    }
}
