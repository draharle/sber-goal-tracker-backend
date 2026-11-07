package ru.goaltracker.kkd.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.goaltracker.kkd.engine.TestData.context;
import static ru.goaltracker.kkd.engine.TestData.goal;
import static ru.goaltracker.kkd.engine.TestData.kr;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Severity;

@DisplayName("GOAL_NO_KR: у активной цели нет ни одного КР")
class GoalNoKrRuleTest {

    private final GoalNoKrRule rule = new GoalNoKrRule();

    @Test
    @DisplayName("пустой список КР у активной цели — критичная проблема; один КР — уже норма")
    void emptyKeyResultList() {
        List<Issue> issues = rule.check(context(
                List.of(goal("G-EMPTY", "U1", "P", "unit"), goal("G-ONE", "U1", "P", "unit")),
                List.of(kr("KR1", "G-ONE", 100))));

        assertThat(issues).singleElement().satisfies(i -> {
            assertThat(i.fingerprint()).isEqualTo("GOAL_NO_KR:G-EMPTY");
            assertThat(i.severity()).isEqualTo(Severity.CRITICAL);
            assertThat(i.actual()).isEqualTo("0");
        });
    }

    @Test
    @DisplayName("черновик, закрытая и отменённая цели без КР — не проблема")
    void inactiveGoalsAreIgnored() {
        List<Issue> issues = rule.check(context(List.of(
                goal("G1", "U1", "P", "unit", "draft"),
                goal("G2", "U1", "P", "unit", "closed"),
                goal("G3", "U1", "P", "unit", "cancelled")), List.of()));
        assertThat(issues).isEmpty();
    }
}
