package ru.goaltracker.kkd.engine;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Данные снимка в удобном для правил виде: активные цели, КР по цели, поиск цели по id.
 * Строится один раз на прогон и передаётся всем правилам.
 */
public final class RuleContext {

    private final LocalDate snapshotDate;
    private final Map<String, Goal> goalsById;
    private final List<Goal> activeGoals;
    private final Map<String, List<KeyResult>> keyResultsByGoal;

    public RuleContext(DataSnapshot snapshot) {
        this.snapshotDate = snapshot.snapshotDate();
        this.goalsById = snapshot.goals().stream()
                .collect(Collectors.toMap(Goal::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        this.activeGoals = snapshot.goals().stream()
                .filter(Goal::isActive)
                .sorted(Comparator.comparing(Goal::id))
                .toList();
        this.keyResultsByGoal = snapshot.keyResults().stream()
                .sorted(Comparator.comparing(KeyResult::id))
                .collect(Collectors.groupingBy(KeyResult::goalId));
    }

    /** Дата, на которую выполняется проверка (от неё считается «давность» факта КР). */
    public LocalDate snapshotDate() {
        return snapshotDate;
    }

    /** Только активные цели — правила применяются к ним. */
    public List<Goal> activeGoals() {
        return activeGoals;
    }

    public List<KeyResult> keyResultsOf(Goal goal) {
        return keyResultsByGoal.getOrDefault(goal.id(), List.of());
    }

    /** Любая цель снимка (в том числе неактивная) — нужна, чтобы проверить ссылку на родителя. */
    public Optional<Goal> goal(String goalId) {
        return goalId == null ? Optional.empty() : Optional.ofNullable(goalsById.get(goalId));
    }
}
