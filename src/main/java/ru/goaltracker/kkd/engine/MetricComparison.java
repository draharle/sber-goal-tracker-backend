package ru.goaltracker.kkd.engine;

import java.math.BigDecimal;

/**
 * Значение метрики в текущем прогоне против прошлого.
 * Нет прошлого значения (первый прогон) или текущего (нет целей) — delta и trend равны null.
 */
public record MetricComparison(BigDecimal value, BigDecimal previous, BigDecimal delta, Trend trend) {

    public static MetricComparison of(BigDecimal value, BigDecimal previous, boolean higherIsBetter) {
        if (value == null || previous == null) {
            return new MetricComparison(value, previous, null, null);
        }
        BigDecimal delta = value.subtract(previous);
        return new MetricComparison(value, previous, delta, Trend.of(delta, higherIsBetter));
    }
}
