package ru.goaltracker.kkd.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import ru.goaltracker.kkd.support.KkdIntegrationTest;

/** Внеплановый прогон меняет историю, поэтому у теста свой контекст и своя база. */
@KkdIntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("POST /api/runs — внеплановый прогон")
class RunExecutionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("данные не менялись: все 13 проблем сохраняются, новых и исправленных нет")
    void manualRunOnUnchangedData() throws Exception {
        mvc.perform(post("/api/runs"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("RUN-004"))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.trigger").value("manual"))
                .andExpect(jsonPath("$.compared_to_run_id").value("RUN-003"))
                .andExpect(jsonPath("$.issues_total").value(13));

        mvc.perform(get("/api/kkd/summary"))
                .andExpect(jsonPath("$.run.id").value("RUN-004"))
                .andExpect(jsonPath("$.compared_to.id").value("RUN-003"))
                .andExpect(jsonPath("$.metrics[0].trend").value("same"))
                .andExpect(jsonPath("$.delta.new").value(0))
                .andExpect(jsonPath("$.delta.persisting").value(13))
                .andExpect(jsonPath("$.delta.resolved").value(0));
    }
}
