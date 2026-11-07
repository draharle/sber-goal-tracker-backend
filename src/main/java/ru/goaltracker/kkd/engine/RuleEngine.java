package ru.goaltracker.kkd.engine;

import java.util.Comparator;
import java.util.List;

/**
 * Движок правил: прогоняет снимок данных через все правила и возвращает общий список проблем.
 * Не зависит от Spring, БД и HTTP — на входе данные, на выходе список.
 */
public final class RuleEngine {

    /** Порядок выдачи: сначала критичные, затем по подразделению, объекту и правилу. */
    public static final Comparator<Issue> ISSUE_ORDER = Comparator
            .comparing(Issue::severity)
            .thenComparing(Issue::unitId)
            .thenComparing(Issue::objectId)
            .thenComparing(Issue::ruleCode);

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<Rule> rules() {
        return rules;
    }

    public List<Issue> check(DataSnapshot snapshot) {
        RuleContext context = new RuleContext(snapshot);
        return rules.stream()
                .flatMap(rule -> rule.check(context).stream())
                .sorted(ISSUE_ORDER)
                .toList();
    }
}
