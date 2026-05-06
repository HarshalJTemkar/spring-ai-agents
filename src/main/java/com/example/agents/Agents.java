package com.example.agents;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.example.agents.AgentProperties.AgentSettings;

/**
 * Three specialised agents that share the same underlying LLM but have
 * different system prompts (= different roles).
 *
 * Retry count and per-call timeout are configured PER agent via
 * {@link AgentProperties} (prefix {@code agents}).
 */
@Component
public class Agents {

    private static final Logger log = LoggerFactory.getLogger(Agents.class);

    private final ChatClient.Builder builder;
    private final AgentProperties props;

    public Agents(ChatClient.Builder builder, AgentProperties props) {
        this.builder = builder;
        this.props = props;
    }

    /** Researcher Agent: allowed to call the webSearch tool. */
    public String researcher(String topic) {
        return run("researcher", () -> builder.build().prompt()
                .system("You are a Research Agent. Use the webSearch tool when you need facts. "
                      + "Return concise bullet-style notes only - no prose.")
                .user("Research this topic: " + topic)
                .toolNames("webSearch")
                .call()
                .content());
    }

    /** Writer Agent: turns research notes into an executive summary. */
    public String writer(String researchNotes) {
        return run("writer", () -> builder.build().prompt()
                .system("You are a Writer Agent. Convert the given notes into a clean, "
                      + "5-bullet executive summary suitable for a busy manager.")
                .user("Notes:\n" + researchNotes)
                .call()
                .content());
    }

    /** Reviewer Agent: polishes grammar and clarity, returns final text. */
    public String reviewer(String draft) {
        return run("reviewer", () -> builder.build().prompt()
                .system("You are a Reviewer Agent. Fix grammar, improve clarity, "
                      + "and ensure each bullet is self-contained. Return ONLY the final text.")
                .user(draft)
                .call()
                .content());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Wraps an LLM call with this agent's retry policy and per-attempt timeout. */
    private String run(String agentName, Callable<String> action) {
        AgentSettings cfg = props.forAgent(agentName);
        RetryTemplate retry = retryTemplateFor(cfg);

        return retry.execute(ctx -> {
            int attempt = ctx.getRetryCount() + 1;
            log.debug("[{}] attempt {}/{} (timeout={}s)",
                    agentName, attempt, cfg.getMaxAttempts(), cfg.getTimeout().getSeconds());
            return callWithTimeout(action, cfg.getTimeout().toMillis(), agentName);
        });
    }

    private RetryTemplate retryTemplateFor(AgentSettings cfg) {
        ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(cfg.getBackoffInitial().toMillis());
        backoff.setMaxInterval(cfg.getBackoffMax().toMillis());
        backoff.setMultiplier(cfg.getBackoffMultiplier());

        SimpleRetryPolicy policy = new SimpleRetryPolicy(Math.max(1, cfg.getMaxAttempts()));

        RetryTemplate t = new RetryTemplate();
        t.setBackOffPolicy(backoff);
        t.setRetryPolicy(policy);
        return t;
    }

    private String callWithTimeout(Callable<String> action, long timeoutMs, String agentName) {
        var executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "agent-" + agentName);
            t.setDaemon(true);
            return t;
        });
        try {
            Future<String> future = executor.submit(action);
            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                future.cancel(true);
                throw new AgentTimeoutException(
                        "Agent '" + agentName + "' timed out after " + timeoutMs + " ms", te);
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
                if (cause instanceof RuntimeException re) throw re;
                throw new RuntimeException(cause);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /** Marker exception so callers can distinguish a timeout from other failures. */
    public static class AgentTimeoutException extends RuntimeException {
        public AgentTimeoutException(String msg, Throwable cause) { super(msg, cause); }
    }
}