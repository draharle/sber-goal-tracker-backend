package ru.goaltracker.kkd.engine.rules;

import java.util.List;

import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Rule;
import ru.goaltracker.kkd.engine.RuleContext;
import ru.goaltracker.kkd.engine.Severity;

/** GOAL_NO_KR (critical): у активной цели нет ни одного ключевого результата. */
public final class GoalNoKrRule implements Rule {

    public static final String CODE = "GOAL_NO_KR";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Severity severity() {
        return Severity.CRITICAL;
    }

    @Override
    public String objectType() {
        return Issue.OBJECT_GOAL;
    }

    @Override
    public List<Issue> check(RuleContext context) {
        return context.activeGoals().stream()
                .filter(goal -> context.keyResultsOf(goal).isEmpty())
                .map(goal -> Issue.forGoal(this, goal,
                        "У активной цели нет ключевых результатов", "0", ">= 1"))
                .toList();
    }
}
