package com.example.agents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the Spring AI multi-agent demo.
 *
 * Trigger the workflow with:
 *   POST http://localhost:8080/agent/run
 *   { "goal": "Competitive analysis of EV cars" }
 */
@SpringBootApplication
@EnableConfigurationProperties(AgentProperties.class)
public class AgenticApp {

    public static void main(String[] args) {
        SpringApplication.run(AgenticApp.class, args);
    }
}
