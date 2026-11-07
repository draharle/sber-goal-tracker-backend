package ru.goaltracker.kkd.engine.rules;

import java.util.List;

import ru.goaltracker.kkd.engine.Rule;

/** Набор правил, который включён в модуле. Новое правило = новый класс + строка здесь + строка в kkd_rule. */
public final class StandardRules {

    private StandardRules() {
    }

    public static List<Rule> all() {
        return List.of(
                new GoalNoKrRule(),
                new KrWeightsSumRule(),
                new CascadeNoParentRule(),
                new KrStaleProgressRule());
    }
}
