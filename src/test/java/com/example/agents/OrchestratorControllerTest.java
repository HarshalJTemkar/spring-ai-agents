package com.example.agents;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer test for {@link OrchestratorController}. The {@link Agents}
 * collaborator is mocked so no real LLM call is made.
 */
@WebMvcTest(OrchestratorController.class)
class OrchestratorControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private Agents agents;

    @Test
    void run_chainsAgentsAndReturnsAllStages() throws Exception {
        when(agents.researcher("EVs")).thenReturn("RAW_NOTES");
        when(agents.writer("RAW_NOTES")).thenReturn("DRAFT");
        when(agents.reviewer("DRAFT")).thenReturn("FINAL");

        mvc.perform(post("/agent/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"goal\":\"EVs\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.goal", is("EVs")))
            .andExpect(jsonPath("$.researchNotes", is("RAW_NOTES")))
            .andExpect(jsonPath("$.draft", is("DRAFT")))
            .andExpect(jsonPath("$.finalOutput", is("FINAL")));

        // Verify pipeline order / data flow: researcher -> writer -> reviewer
        verify(agents).researcher(eq("EVs"));
        verify(agents).writer(eq("RAW_NOTES"));
        verify(agents).reviewer(eq("DRAFT"));
    }

    @Test
    void run_propagatesAgentFailure() {
        when(agents.researcher("X"))
                .thenThrow(new RuntimeException("LLM unavailable"));

        // MockMvc surfaces unhandled controller exceptions to the caller; assert
        // that the underlying agent failure reaches us (root cause check).
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mvc.perform(post("/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"X\"}")))
            .hasRootCauseInstanceOf(RuntimeException.class)
            .hasRootCauseMessage("LLM unavailable");
    }
}
