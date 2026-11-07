package ru.goaltracker.kkd.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.goaltracker.kkd.engine.rules.CascadeNoParentRule;

/**
 * Расчёт метрик подразделения по снимку данных и найденным проблемам.
 * Подразделение считается вместе со всеми дочерними ({@link UnitTree#descendantsOrSelf}).
 * Нет активных целей — доли равны null («не из чего считать»), а не 0 («всё плохо»).
 */
public final class MetricsCalculator {

    /** Правила, нарушение которых означает «цель выпала из каскада». */
    static final Set<String> CASCADE_RULES = Set.of(CascadeNoParentRule.CODE);

    private MetricsCalculator() {
    }

    public static Map<Metric, BigDecimal> calculate(DataSnapshot snapshot, List<Issue> issues,
                                                    UnitTree tree, String unitId) {
        Set<String> scope = tree.descendantsOrSelf(unitId);
        List<Goal> goals = snapshot.goals().stream()
                .filter(Goal::isActive)
                .filter(g -> scope.contains(g.unitId()))
                .toList();
        Set<String> goalIds = goals.stream().map(Goal::id).collect(Collectors.toSet());
        List<Issue> scoped = issues.stream().filter(i -> goalIds.contains(i.goalId())).toList();

        Set<String> goalsWithIssues = scoped.stream().map(Issue::goalId).collect(Collectors.toSet());
        Set<String> goalsOutOfCascade = scoped.stream()
                .filter(i -> CASCADE_RULES.contains(i.ruleCode()))
                .map(Issue::goalId)
                .collect(Collectors.toSet());
        long belowBlock = goals.stream().filter(g -> !g.isBlockLevel()).count();

        Map<Metric, BigDecimal> result = new EnumMap<>(Metric.class);
        result.put(Metric.CLEAN_GOALS_SHARE, share(goals.size() - goalsWithIssues.size(), goals.size()));
        result.put(Metric.CASCADE_COVERAGE, share(belowBlock - goalsOutOfCascade.size(), belowBlock));
        result.put(Metric.ISSUES_TOTAL, BigDecimal.valueOf(scoped.size()));
        return result;
    }

    /** Метрики для каждого подразделения оргструктуры — так их сохраняет прогон. */
    public static Map<String, Map<Metric, BigDecimal>> calculateForAllUnits(DataSnapshot snapshot,
                                                                         List<Issue> issues, UnitTree tree) {
        Map<String, Map<Metric, BigDecimal>> result = new LinkedHashMap<>();
        for (Unit unit : tree.units()) {
            result.put(unit.id(), calculate(snapshot, issues, tree, unit.id()));
        }
        return result;
    }

    /** Доля в процентах с одним знаком после запятой; при пустом знаменателе — null. */
    static BigDecimal share(long part, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(part * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
