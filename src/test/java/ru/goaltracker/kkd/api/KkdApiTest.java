package ru.goaltracker.kkd.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.goaltracker.kkd.engine.DataSnapshot;
import ru.goaltracker.kkd.engine.MetricsCalculator;
import ru.goaltracker.kkd.engine.RuleEngine;
import ru.goaltracker.kkd.engine.UnitTree;
import ru.goaltracker.kkd.repository.GoalDataRepository;
import ru.goaltracker.kkd.repository.IssueRepository;
import ru.goaltracker.kkd.repository.RuleRepository;
import ru.goaltracker.kkd.repository.RunMetricRepository;
import ru.goaltracker.kkd.support.KkdIntegrationTest;

/**
 * API на демо-данных: RUN-001 (из сидов) → RUN-002 (упал) → RUN-003 (движок при старте, сравнение с RUN-001).
 * Ожидаемые числа разобраны в README, раздел «Стартовые данные».
 */
@KkdIntegrationTest
@DisplayName("REST API модуля ККД")
class KkdApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /api/runs — история прогонов, упавший пропущен при сравнении")
    void runs() throws Exception {
        mvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].id").value("RUN-003"))
                .andExpect(jsonPath("$.items[0].status").value("completed"))
                .andExpect(jsonPath("$.items[0].trigger").value("startup"))
                .andExpect(jsonPath("$.items[0].snapshot_date").value("2026-10-26"))
                .andExpect(jsonPath("$.items[0].compared_to_run_id").value("RUN-001"))
                .andExpect(jsonPath("$.items[0].issues_total").value(13))
                .andExpect(jsonPath("$.items[1].status").value("failed"))
                .andExpect(jsonPath("$.items[1].issues_total").value(nullValue()))
                .andExpect(jsonPath("$.items[2].id").value("RUN-001"))
                .andExpect(jsonPath("$.items[2].compared_to_run_id").value(nullValue()))
                .andExpect(jsonPath("$.items[2].issues_total").value(12));
    }

    @Test
    @DisplayName("GET /api/kkd/summary — последний прогон по корню оргструктуры")
    void summaryLatestForRoot() throws Exception {
        mvc.perform(get("/api/kkd/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.id").value("RUN-003"))
                .andExpect(jsonPath("$.compared_to.id").value("RUN-001"))
                .andExpect(jsonPath("$.compared_to.snapshot_date").value("2026-10-12"))
                .andExpect(jsonPath("$.unit.id").value("B2B"))
                .andExpect(jsonPath("$.metrics[0].key").value("clean_goals_share"))
                .andExpect(jsonPath("$.metrics[0].value").value(43.5))
                .andExpect(jsonPath("$.metrics[0].previous").value(47.8))
                .andExpect(jsonPath("$.metrics[0].delta").value(-4.3))
                .andExpect(jsonPath("$.metrics[0].trend").value("worse"))
                .andExpect(jsonPath("$.metrics[0].higher_is_better").value(true))
                .andExpect(jsonPath("$.metrics[1].key").value("cascade_coverage"))
                .andExpect(jsonPath("$.metrics[1].value").value(86.4))
                .andExpect(jsonPath("$.metrics[1].trend").value("same"))
                .andExpect(jsonPath("$.metrics[2].key").value("issues_total"))
                .andExpect(jsonPath("$.metrics[2].value").value(13))
                .andExpect(jsonPath("$.metrics[2].previous").value(12))
                .andExpect(jsonPath("$.metrics[2].delta").value(1))
                .andExpect(jsonPath("$.metrics[2].trend").value("worse"))
                .andExpect(jsonPath("$.issues_by_severity[0].code").value("critical"))
                .andExpect(jsonPath("$.issues_by_severity[0].current").value(2))
                .andExpect(jsonPath("$.issues_by_severity[0].delta").value(0))
                .andExpect(jsonPath("$.issues_by_severity[1].current").value(6))
                .andExpect(jsonPath("$.issues_by_severity[2].code").value("minor"))
                .andExpect(jsonPath("$.issues_by_severity[2].current").value(5))
                .andExpect(jsonPath("$.issues_by_severity[2].previous").value(4))
                .andExpect(jsonPath("$.issues_by_severity[2].delta").value(1))
                .andExpect(jsonPath("$.delta.new").value(5))
                .andExpect(jsonPath("$.delta.persisting").value(8))
                .andExpect(jsonPath("$.delta.resolved").value(4));
    }

    @Test
    @DisplayName("метрики департамента включают дочерние управления; пустое подразделение — null")
    void summaryCascadeAndEmptyUnit() throws Exception {
        mvc.perform(get("/api/kkd/summary").param("unit_id", "HQ-CIB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics[0].value").value(40.9))
                .andExpect(jsonPath("$.metrics[0].previous").value(45.5))
                .andExpect(jsonPath("$.metrics[2].value").value(13));
        mvc.perform(get("/api/kkd/summary").param("unit_id", "CIB-KEY"))
                .andExpect(jsonPath("$.metrics[0].value").value(44.4))
                .andExpect(jsonPath("$.metrics[0].trend").value("better"))
                .andExpect(jsonPath("$.delta.resolved").value(2));
        mvc.perform(get("/api/kkd/summary").param("unit_id", "CIB-TRADE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics[0].value").value(nullValue()))
                .andExpect(jsonPath("$.metrics[0].delta").value(nullValue()))
                .andExpect(jsonPath("$.metrics[0].trend").value(nullValue()))
                .andExpect(jsonPath("$.metrics[2].value").value(nullValue()))
                .andExpect(jsonPath("$.issues_by_severity[0].current").value(0));
    }

    @Test
    @DisplayName("первый прогон: сравнения нет — previous, delta и счётчики дельты равны null")
    void summaryFirstRun() throws Exception {
        mvc.perform(get("/api/kkd/summary").param("run_id", "RUN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compared_to").value(nullValue()))
                .andExpect(jsonPath("$.metrics[0].value").value(47.8))
                .andExpect(jsonPath("$.metrics[0].previous").value(nullValue()))
                .andExpect(jsonPath("$.metrics[0].delta").value(nullValue()))
                .andExpect(jsonPath("$.issues_by_severity[0].previous").value(nullValue()))
                .andExpect(jsonPath("$.delta.new").value(nullValue()))
                .andExpect(jsonPath("$.delta.resolved").value(nullValue()));
    }

    @Test
    @DisplayName("GET /api/kkd/issues — фильтры по подразделению (с дочерними), важности, правилу и пагинация")
    void issues() throws Exception {
        mvc.perform(get("/api/kkd/issues").param("unit_id", "CIB-REG").param("severity", "minor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run_id").value("RUN-003"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].object_id").value("KR-025"))
                .andExpect(jsonPath("$.items[0].actual").value("46"))
                .andExpect(jsonPath("$.items[0].delta_status").value("new"))
                .andExpect(jsonPath("$.items[0].goal_title").value("Выполнение плана продаж управления"))
                .andExpect(jsonPath("$.items[2].object_id").value("KR-034"))
                .andExpect(jsonPath("$.items[2].delta_status").value("persisting"));
        mvc.perform(get("/api/kkd/issues").param("rule_code", "GOAL_NO_KR,KR_WEIGHTS_SUM"))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.items[0].severity").value("critical"));
        mvc.perform(get("/api/kkd/issues").param("limit", "5").param("offset", "10"))
                .andExpect(jsonPath("$.total").value(13))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.limit").value(5))
                .andExpect(jsonPath("$.offset").value(10));
    }

    @ParameterizedTest(name = "{0} → {1} {2}")
    @CsvSource({
            "/api/kkd/summary?run_id=RUN-999,       404, RUN_NOT_FOUND",
            "/api/kkd/summary?unit_id=NOPE,         404, UNIT_NOT_FOUND",
            "/api/kkd/issues?run_id=RUN-404,        404, RUN_NOT_FOUND",
            "/api/kkd/summary?run_id=RUN-002,       409, RUN_FAILED",
            "/api/kkd/issues?run_id=RUN-002,        409, RUN_FAILED",
            "/api/kkd/summary?run_id=abc,           422, INVALID_PARAM",
            "/api/kkd/summary?unit_id=bad id,       422, INVALID_PARAM",
            "/api/kkd/issues?limit=0,               422, INVALID_PARAM",
            "/api/kkd/issues?limit=201,             422, INVALID_PARAM",
            "/api/kkd/issues?offset=-1,             422, INVALID_PARAM",
            "/api/kkd/issues?limit=abc,             422, INVALID_PARAM",
            "/api/kkd/issues?severity=urgent,       422, INVALID_PARAM",
            "/api/kkd/issues?rule_code=GOAL_NO_OWNER, 422, INVALID_PARAM",
            "/api/runs?limit=101,                   422, INVALID_PARAM",
            "/api/nope,                             404, NOT_FOUND"})
    @DisplayName("ошибки — единый формат {error: {code, message}}")
    void errors(String url, int httpStatus, String code) throws Exception {
        mvc.perform(get(url))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Autowired
    private GoalDataRepository goalData;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private RunMetricRepository metricRepository;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private RuleEngine engine;

    @Test
    @DisplayName("сиды согласованы: метрики RUN-001 пересчитываются движком, правила совпадают со справочником")
    void seedConsistency() {
        // Набор целей между RUN-001 и RUN-003 не менялся, поэтому метрики RUN-001 = расчёт по его проблемам
        DataSnapshot snapshot = goalData.loadSnapshot(java.time.LocalDate.of(2026, 10, 12));
        var recalculated = MetricsCalculator.calculateForAllUnits(snapshot,
                issueRepository.findByRun("RUN-001"), new UnitTree(snapshot.units()));
        assertThat(metricRepository.findAllByRun("RUN-001")).isEqualTo(recalculated);

        Map<String, String> catalog = ruleRepository.findAll().stream()
                .collect(Collectors.toMap(RuleRepository.RuleRecord::code, RuleRepository.RuleRecord::severity));
        Map<String, String> implemented = engine.rules().stream()
                .collect(Collectors.toMap(r -> r.code(), r -> r.severity().code()));
        assertThat(implemented).isEqualTo(catalog);
    }
}
