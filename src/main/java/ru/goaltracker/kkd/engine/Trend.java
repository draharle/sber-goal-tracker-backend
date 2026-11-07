package ru.goaltracker.kkd.engine;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Оценка изменения метрики. Знак дельты ≠ оценка: рост «Найдено проблем» — это хуже,
 * рост «Доли корректных целей» — лучше. Поэтому учитываем higherIsBetter.
 */
public enum Trend {
    BETTER, WORSE, SAME;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Trend of(BigDecimal delta, boolean higherIsBetter) {
        int sign = delta.signum();
        if (sign == 0) {
            return SAME;
        }
        return (sign > 0) == higherIsBetter ? BETTER : WORSE;
    }
}
