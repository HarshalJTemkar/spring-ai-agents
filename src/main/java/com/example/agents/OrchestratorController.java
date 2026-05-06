package com.example.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Orchestrator: chains the three agents sequentially.
 *
 *   POST /agent/run
 *   { "goal": "Competitive analysis of EV cars" }
 *
 * Researcher -> Writer -> Reviewer -> Final response.
 */
@RestController
@RequestMapping("/agent")
public class OrchestratorController {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorController.class);

    private final Agents agents;

    public OrchestratorController(Agents agents) {
        this.agents = agents;
    }

    @PostMapping("/run")
    public Response run(@RequestBody Request req) {
        log.info("Received goal: {}", req.goal());

        String research = agents.researcher(req.goal());
        log.debug("Researcher output:\n{}", research);

        String draft = agents.writer(research);
        log.debug("Writer draft:\n{}", draft);

        String finalOut = agents.reviewer(draft);
        log.debug("Reviewer final:\n{}", finalOut);

        return new Response(req.goal(), research, draft, finalOut);
    }

    public record Request(String goal) {}

    public record Response(String goal, String researchNotes, String draft, String finalOutput) {}
}
