package ru.goaltracker.kkd.engine.rules;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import ru.goaltracker.kkd.engine.Goal;
import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.KeyResult;
import ru.goaltracker.kkd.engine.Rule;
import ru.goaltracker.kkd.engine.RuleContext;
import ru.goaltracker.kkd.engine.Severity;

/**
 * KR_STALE_PROGRESS (minor): факт КР не обновлялся более 45 дней.
 * Давность = дата снимка − дата обновления факта, в днях. 45 — ещё норма, 46 — проблема.
 */
public final class KrStaleProgressRule implements Rule {

    public static final String CODE = "KR_STALE_PROGRESS";
    public static final int MAX_AGE_DAYS = 45;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public String objectType() {
        return Issue.OBJECT_KEY_RESULT;
    }

    @Override
    public List<Issue> check(RuleContext context) {
        List<Issue> issues = new ArrayList<>();
        for (Goal goal : context.activeGoals()) {
            for (KeyResult kr : context.keyResultsOf(goal)) {
                long age = ChronoUnit.DAYS.between(kr.factUpdatedAt(), context.snapshotDate());
                if (age > MAX_AGE_DAYS) {
                    issues.add(Issue.forKeyResult(this, goal, kr,
                            "Факт обновлялся " + age + " дн. назад (" + kr.factUpdatedAt() + ")",
                            String.valueOf(age), "<= " + MAX_AGE_DAYS));
                }
            }
        }
        return issues;
    }
}
