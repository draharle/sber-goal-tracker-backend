package ru.goaltracker.kkd.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.goaltracker.kkd.engine.TestData.context;
import static ru.goaltracker.kkd.engine.TestData.goal;
import static ru.goaltracker.kkd.engine.TestData.kr;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Severity;

@DisplayName("KR_WEIGHTS_SUM: сумма весов КР должна быть ровно 100%")
class KrWeightsSumRuleTest {

    private final KrWeightsSumRule rule = new KrWeightsSumRule();

    @ParameterizedTest(name = "веса {0} + {1} = {2}%, проблема: {3}")
    @CsvSource({
            "50, 49, 99,  true",
            "50, 50, 100, false",
            "50, 51, 101, true"})
    void boundaryWeights(int w1, int w2, int sum, boolean expectIssue) {
        List<Issue> issues = rule.check(context(
                List.of(goal("G1", "U1", "P", "unit")),
                List.of(kr("KR1", "G1", w1), kr("KR2", "G1", w2))));

        if (expectIssue) {
            assertThat(issues).singleElement().satisfies(i -> {
                assertThat(i.fingerprint()).isEqualTo("KR_WEIGHTS_SUM:G1");
                assertThat(i.severity()).isEqualTo(Severity.MAJOR);
                assertThat(i.actual()).isEqualTo(String.valueOf(sum));
                assertThat(i.expected()).isEqualTo("100");
            });
        } else {
            assertThat(issues).isEmpty();
        }
    }

    @Test
    @DisplayName("цель с пустым списком КР этим правилом не проверяется (это GOAL_NO_KR)")
    void goalWithoutKeyResultsIsNotChecked() {
        assertThat(rule.check(context(List.of(goal("G1", "U1", "P", "unit")), List.of()))).isEmpty();
    }

    @Test
    @DisplayName("неактивная цель с неверными весами не проверяется")
    void inactiveGoalIsIgnored() {
        List<Issue> issues = rule.check(context(
                List.of(goal("G1", "U1", "P", "unit", "draft")),
                List.of(kr("KR1", "G1", 30))));
        assertThat(issues).isEmpty();
    }
}
