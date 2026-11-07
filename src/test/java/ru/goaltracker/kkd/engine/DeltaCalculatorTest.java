package ru.goaltracker.kkd.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Дельта между прогонами и оценка «лучше / хуже»")
class DeltaCalculatorTest {

    private static Issue issue(String rule, String objectId) {
        return new Issue(rule, Severity.MAJOR, "goal", objectId, objectId, null, "U1", "", null, null);
    }

    @Test
    @DisplayName("первый прогон: сравнивать не с чем — new / persisting / resolved равны null, а не 0")
    void firstRunHasNoComparison() {
        DeltaCounts delta = DeltaCalculator.compare(List.of(issue("GOAL_NO_KR", "G1")), null);
        assertThat(delta.newCount()).isNull();
        assertThat(delta.persisting()).isNull();
        assertThat(delta.resolved()).isNull();
        assertThat(DeltaCalculator.statusOf("GOAL_NO_KR:G1", null)).isNull();
    }

    @Test
    @DisplayName("та же проблема = то же правило + тот же объект")
    void newPersistingResolved() {
        List<Issue> previous = List.of(issue("GOAL_NO_KR", "G1"), issue("KR_WEIGHTS_SUM", "G2"),
                issue("CASCADE_NO_PARENT", "G3"));
        List<Issue> current = List.of(issue("GOAL_NO_KR", "G1"), issue("KR_WEIGHTS_SUM", "G3"),
                issue("CASCADE_NO_PARENT", "G4"));

        DeltaCounts delta = DeltaCalculator.compare(current, previous);

        // GOAL_NO_KR:G1 — сохраняется; KR_WEIGHTS_SUM:G3 и CASCADE_NO_PARENT:G4 — новые
        // (то же правило на другом объекте — другая проблема); KR_WEIGHTS_SUM:G2 и CASCADE_NO_PARENT:G3 — исправлены
        assertThat(delta).isEqualTo(new DeltaCounts(2, 1, 2));
        Set<String> prevFps = DeltaCalculator.fingerprints(previous);
        assertThat(DeltaCalculator.statusOf("GOAL_NO_KR:G1", prevFps)).isEqualTo(DeltaStatus.PERSISTING);
        assertThat(DeltaCalculator.statusOf("CASCADE_NO_PARENT:G4", prevFps)).isEqualTo(DeltaStatus.NEW);
    }

    @Test
    @DisplayName("знак дельты не равен оценке: рост числа проблем — хуже, рост доли — лучше")
    void trendRespectsHigherIsBetter() {
        MetricComparison share = MetricComparison.of(new BigDecimal("56.5"), new BigDecimal("62.7"), true);
        assertThat(share.delta()).isEqualByComparingTo("-6.2");
        assertThat(share.trend()).isEqualTo(Trend.WORSE);

        MetricComparison issues = MetricComparison.of(new BigDecimal("10"), new BigDecimal("12"), false);
        assertThat(issues.delta()).isEqualByComparingTo("-2");
        assertThat(issues.trend()).isEqualTo(Trend.BETTER);

        assertThat(MetricComparison.of(new BigDecimal("80.0"), new BigDecimal("80.0"), true).trend())
                .isEqualTo(Trend.SAME);

        MetricComparison first = MetricComparison.of(new BigDecimal("50.0"), null, true);
        assertThat(first.previous()).isNull();
        assertThat(first.delta()).isNull();
        assertThat(first.trend()).isNull();
    }

    @Test
    @DisplayName("id проблемы стабилен между прогонами и зависит только от отпечатка")
    void publicIdIsStable() {
        Issue a = issue("GOAL_NO_KR", "G1");
        Issue b = new Issue("GOAL_NO_KR", Severity.CRITICAL, "goal", "G1", "G1", null, "U2", "другой текст", "0", ">= 1");
        assertThat(a.publicId()).isEqualTo(b.publicId()).matches("ISS-[0-9A-F]{10}");
        assertThat(a.publicId()).isNotEqualTo(issue("GOAL_NO_KR", "G2").publicId());
    }
}
