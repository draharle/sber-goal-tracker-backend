package ru.goaltracker.kkd.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.goaltracker.kkd.engine.TestData.UNITS;
import static ru.goaltracker.kkd.engine.TestData.goal;
import static ru.goaltracker.kkd.engine.TestData.kr;
import static ru.goaltracker.kkd.engine.TestData.snapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.goaltracker.kkd.engine.rules.StandardRules;

@DisplayName("Метрики подразделения с учётом дочерних")
class MetricsCalculatorTest {

    private final UnitTree tree = new UnitTree(UNITS);
    private final RuleEngine engine = new RuleEngine(StandardRules.all());

    /**
     * HQ: G-HQ (норма). U1: G-U1 (норма), G-U1-BAD (нет родителя). U2: G-U2 (нет КР), G-U2-DRAFT (не считается).
     * Блок: G-BLOCK (норма).
     */
    private final DataSnapshot snapshot = snapshot(
            List.of(goal("G-BLOCK", "B2B", null, "block"),
                    goal("G-HQ", "HQ", "G-BLOCK", "hq"),
                    goal("G-U1", "U1", "G-HQ", "unit"),
                    goal("G-U1-BAD", "U1", null, "unit"),
                    goal("G-U2", "U2", "G-HQ", "unit"),
                    goal("G-U2-DRAFT", "U2", null, "unit", "draft")),
            List.of(kr("KR1", "G-BLOCK", 100), kr("KR2", "G-HQ", 100), kr("KR3", "G-U1", 100),
                    kr("KR4", "G-U1-BAD", 100)));

    @Test
    @DisplayName("департамент считается вместе с дочерними управлениями")
    void departmentIncludesChildUnits() {
        List<Issue> issues = engine.check(snapshot);

        Map<Metric, BigDecimal> hq = MetricsCalculator.calculate(snapshot, issues, tree, "HQ");
        // 4 активные цели в HQ+U1+U2, проблемные 2 (G-U1-BAD, G-U2) → 50.0%
        assertThat(hq.get(Metric.CLEAN_GOALS_SHARE)).isEqualByComparingTo("50.0");
        // все 4 ниже блока, из каскада выпала 1 (G-U1-BAD) → 75.0%
        assertThat(hq.get(Metric.CASCADE_COVERAGE)).isEqualByComparingTo("75.0");
        assertThat(hq.get(Metric.ISSUES_TOTAL)).isEqualByComparingTo("2");

        Map<Metric, BigDecimal> u1 = MetricsCalculator.calculate(snapshot, issues, tree, "U1");
        assertThat(u1.get(Metric.CLEAN_GOALS_SHARE)).isEqualByComparingTo("50.0");
        assertThat(u1.get(Metric.ISSUES_TOTAL)).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("цель блока не входит в знаменатель покрытия каскадом; доля округляется до 0,1")
    void blockGoalExcludedFromCascadeCoverage() {
        List<Issue> issues = engine.check(snapshot);
        Map<Metric, BigDecimal> root = MetricsCalculator.calculate(snapshot, issues, tree, "B2B");
        // 5 активных целей, проблемных 2 → 3/5 = 60.0; ниже блока 4, выпала 1 → 75.0
        assertThat(root.get(Metric.CLEAN_GOALS_SHARE)).isEqualByComparingTo("60.0");
        assertThat(root.get(Metric.CASCADE_COVERAGE)).isEqualByComparingTo("75.0");
        assertThat(MetricsCalculator.share(2, 3)).isEqualByComparingTo("66.7");
    }

    @Test
    @DisplayName("в подразделении нет активных целей — все три метрики null, а не 0")
    void emptyUnitGivesNull() {
        Map<Metric, BigDecimal> empty = MetricsCalculator.calculate(snapshot, engine.check(snapshot), tree, "EMPTY");
        assertThat(empty).containsKeys(Metric.values());
        assertThat(empty.get(Metric.CLEAN_GOALS_SHARE)).isNull();
        assertThat(empty.get(Metric.CASCADE_COVERAGE)).isNull();
        assertThat(empty.get(Metric.ISSUES_TOTAL)).isNull();
    }

    @Test
    @DisplayName("дерево подразделений обходится рекурсивно на любую глубину")
    void unitTreeDescendants() {
        assertThat(tree.descendantsOrSelf("B2B")).containsExactlyInAnyOrder("B2B", "HQ", "U1", "U2", "EMPTY");
        assertThat(tree.descendantsOrSelf("U1")).containsExactly("U1");
        assertThat(tree.descendantsOrSelf("NOPE")).isEmpty();
        assertThat(tree.root()).map(Unit::id).hasValue("B2B");
    }
}
