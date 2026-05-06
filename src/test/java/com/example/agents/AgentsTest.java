package com.example.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

import com.example.agents.AgentProperties.AgentSettings;

/**
 * Unit tests for {@link Agents}: verify per-agent retry count, per-attempt
 * timeout, and that the right system prompt / tool wiring reaches the
 * underlying {@link ChatClient}.
 */
class AgentsTest {

    private ChatClient.Builder builder;
    private AgentProperties props;

    @BeforeEach
    void setUp() {
        // Deep stubs: builder.build().prompt().system(...).user(...).toolNames(...).call().content()
        builder = mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
        props = new AgentProperties();
    }

    // ---------------------------------------------------------------------
    // Happy paths
    // ---------------------------------------------------------------------

    @Test
    void researcher_returnsContent_andUsesWebSearchTool() {
        when(builder.build().prompt().system(anyString()).user(anyString())
                .toolNames(anyString()).call().content())
                .thenReturn("notes");

        String out = new Agents(builder, props).researcher("EVs");

        assertThat(out).isEqualTo("notes");
        // Tool name must be "webSearch"
        verify(builder.build().prompt().system(anyString()).user(anyString()))
                .toolNames("webSearch");
    }

    @Test
    void writer_returnsContent() {
        when(builder.build().prompt().system(anyString()).user(anyString())
                .call().content())
                .thenReturn("summary");

        assertThat(new Agents(builder, props).writer("notes")).isEqualTo("summary");
    }

    @Test
    void reviewer_returnsContent() {
        when(builder.build().prompt().system(anyString()).user(anyString())
                .call().content())
                .thenReturn("polished");

        assertThat(new Agents(builder, props).reviewer("draft")).isEqualTo("polished");
    }

    // ---------------------------------------------------------------------
    // Retry behaviour
    // ---------------------------------------------------------------------

    @Test
    void writer_retriesUpToMaxAttempts_thenSucceeds() {
        // Configure writer with 3 attempts, fast backoff, generous timeout.
        AgentSettings s = fastSettings(3, Duration.ofSeconds(5));
        props.setWriter(s);

        AtomicInteger calls = new AtomicInteger();
        when(builder.build().prompt().system(anyString()).user(anyString())
                .call().content())
                .thenAnswer(inv -> {
                    int n = calls.incrementAndGet();
                    if (n < 3) throw new RuntimeException("transient #" + n);
                    return "ok";
                });

        String out = new Agents(builder, props).writer("notes");

        assertThat(out).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void writer_givesUpAfterMaxAttempts() {
        AgentSettings s = fastSettings(2, Duration.ofSeconds(5));
        props.setWriter(s);

        AtomicInteger calls = new AtomicInteger();
        when(builder.build().prompt().system(anyString()).user(anyString())
                .call().content())
                .thenAnswer(inv -> {
                    calls.incrementAndGet();
                    throw new RuntimeException("boom");
                });

        assertThatThrownBy(() -> new Agents(builder, props).writer("notes"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void maxAttemptsOne_meansSingleCall_noRetry() {
        AgentSettings s = fastSettings(1, Duration.ofSeconds(5));
        props.setReviewer(s);

        AtomicInteger calls = new AtomicInteger();
        when(builder.build().prompt().system(anyString()).user(anyString())
                .call().content())
                .thenAnswer(inv -> {
                    calls.incrementAndGet();
                    throw new RuntimeException("nope");
                });

        assertThatThrownBy(() -> new Agents(builder, props).reviewer("x"))
                .isInstanceOf(RuntimeException.class);

        assertThat(calls.get()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------
    // Timeout behaviour
    // ---------------------------------------------------------------------

    @Test
    void writer_timesOut_whenAttemptExceedsBudget() {
        // Tight 200ms timeout, 1 attempt -> instant failure.
        AgentSettings s = fastSettings(1, Duration.ofMillis(200));
        props.setWriter(s);

        when(builder.build().prompt().system(anyString()).user(anyString())
                .call().content())
                .thenAnswer(inv -> {
                    Thread.sleep(2_000);   // far longer than the timeout
                    return "too late";
                });

        assertThatThrownBy(() -> new Agents(builder, props).writer("notes"))
                .isInstanceOf(Agents.AgentTimeoutException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void timeoutCountsAsFailure_andTriggersRetry() {
        // 2 attempts, 200ms timeout each. First attempt sleeps -> timeout.
        // Second attempt returns fast -> success.
        AgentSettings s = fastSettings(2, Duration.ofMillis(200));
        props.setWriter(s);

        AtomicInteger calls = new AtomicInteger();
        when(builder.build().prompt().system(anyString()).user(anyString())
                .call().content())
                .thenAnswer(inv -> {
                    int n = calls.incrementAndGet();
                    if (n == 1) {
                        Thread.sleep(2_000);
                        return "never";
                    }
                    return "fast";
                });

        String out = new Agents(builder, props).writer("notes");
        assertThat(out).isEqualTo("fast");
        assertThat(calls.get()).isGreaterThanOrEqualTo(2);
        verify(builder.build().prompt().system(anyString()).user(anyString())
                .call(), atLeast(2)).content();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static AgentSettings fastSettings(int attempts, Duration timeout) {
        AgentSettings s = new AgentSettings();
        s.setMaxAttempts(attempts);
        s.setTimeout(timeout);
        s.setBackoffInitial(Duration.ofMillis(1));
        s.setBackoffMax(Duration.ofMillis(2));
        s.setBackoffMultiplier(1.0);
        return s;
    }
}
