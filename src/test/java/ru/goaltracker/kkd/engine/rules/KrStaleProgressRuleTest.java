package ru.goaltracker.kkd.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.goaltracker.kkd.engine.TestData.SNAPSHOT_DATE;
import static ru.goaltracker.kkd.engine.TestData.context;
import static ru.goaltracker.kkd.engine.TestData.goal;
import static ru.goaltracker.kkd.engine.TestData.kr;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Severity;

@DisplayName("KR_STALE_PROGRESS: факт КР не обновлялся более 45 дней")
class KrStaleProgressRuleTest {

    private final KrStaleProgressRule rule = new KrStaleProgressRule();

    @Test
    @DisplayName("45 дней — ещё норма, 46 — проблема")
    void boundary45vs46Days() {
        List<Issue> issues = rule.check(context(
                List.of(goal("G1", "U1", "P", "unit")),
                List.of(kr("KR-45", "G1", 50, SNAPSHOT_DATE.minusDays(45)),
                        kr("KR-46", "G1", 50, SNAPSHOT_DATE.minusDays(46)))));

        assertThat(issues).singleElement().satisfies(i -> {
            assertThat(i.fingerprint()).isEqualTo("KR_STALE_PROGRESS:KR-46");
            assertThat(i.objectType()).isEqualTo(Issue.OBJECT_KEY_RESULT);
            assertThat(i.goalId()).isEqualTo("G1");
            assertThat(i.severity()).isEqualTo(Severity.MINOR);
            assertThat(i.actual()).isEqualTo("46");
            assertThat(i.expected()).isEqualTo("<= 45");
        });
    }

    @Test
    @DisplayName("КР закрытой цели не проверяются, даже если факт очень старый")
    void keyResultsOfInactiveGoalAreIgnored() {
        List<Issue> issues = rule.check(context(
                List.of(goal("G1", "U1", "P", "unit", "closed")),
                List.of(kr("KR1", "G1", 100, SNAPSHOT_DATE.minusDays(200)))));
        assertThat(issues).isEmpty();
    }
}
