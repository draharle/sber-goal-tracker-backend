package ru.goaltracker.kkd.engine.rules;

import java.util.List;

import ru.goaltracker.kkd.engine.Goal;
import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Rule;
import ru.goaltracker.kkd.engine.RuleContext;
import ru.goaltracker.kkd.engine.Severity;

/**
 * CASCADE_NO_PARENT (major): цель уровня ниже блока (hq / unit / employee) не привязана
 * к цели вышестоящего уровня. Цель блока — вершина каскада, у неё родителя нет по определению.
 * Срабатывает, если родитель не указан или указан, но такой цели в данных нет.
 * Неверный уровень родителя и закрытый родитель — отдельные правила эталона
 * (CASCADE_WRONG_LEVEL, CASCADE_PARENT_INACTIVE), здесь не проверяются.
 */
public final class CascadeNoParentRule implements Rule {

    public static final String CODE = "CASCADE_NO_PARENT";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public String objectType() {
        return Issue.OBJECT_GOAL;
    }

    @Override
    public List<Issue> check(RuleContext context) {
        return context.activeGoals().stream()
                .filter(goal -> !goal.isBlockLevel())
                .filter(goal -> context.goal(goal.parentId()).isEmpty())
                .map(goal -> Issue.forGoal(this, goal, details(goal), goal.parentId(), "цель вышестоящего уровня"))
                .toList();
    }

    private static String details(Goal goal) {
        return goal.parentId() == null
                ? "Родительская цель не указана"
                : "Родительская цель " + goal.parentId() + " не найдена";
    }
}
