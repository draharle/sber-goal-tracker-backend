package ru.goaltracker.kkd.engine.rules;

import java.util.ArrayList;
import java.util.List;

import ru.goaltracker.kkd.engine.Goal;
import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.KeyResult;
import ru.goaltracker.kkd.engine.Rule;
import ru.goaltracker.kkd.engine.RuleContext;
import ru.goaltracker.kkd.engine.Severity;

/**
 * KR_WEIGHTS_SUM (major): сумма весов КР цели не равна 100%.
 * Цель без КР этим правилом не проверяется — это уже GOAL_NO_KR, одна причина не даёт двух проблем.
 */
public final class KrWeightsSumRule implements Rule {

    public static final String CODE = "KR_WEIGHTS_SUM";
    public static final int EXPECTED_SUM = 100;

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
        List<Issue> issues = new ArrayList<>();
        for (Goal goal : context.activeGoals()) {
            List<KeyResult> keyResults = context.keyResultsOf(goal);
            if (keyResults.isEmpty()) {
                continue;
            }
            int sum = keyResults.stream().mapToInt(KeyResult::weight).sum();
            if (sum != EXPECTED_SUM) {
                issues.add(Issue.forGoal(this, goal,
                        "Сумма весов КР = " + sum + "%, ожидается " + EXPECTED_SUM + "%",
                        String.valueOf(sum), String.valueOf(EXPECTED_SUM)));
            }
        }
        return issues;
    }
}
