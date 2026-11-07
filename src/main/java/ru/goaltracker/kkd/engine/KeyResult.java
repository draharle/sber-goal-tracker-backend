package ru.goaltracker.kkd.engine;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ключевой результат (КР) цели.
 *
 * @param weight        вес КР в процентах (целое число); сумма весов КР цели должна быть 100
 * @param factUpdatedAt когда последний раз обновляли фактическое значение
 */
public record KeyResult(String id, String goalId, String title, String metricUnit,
                        BigDecimal target, BigDecimal fact, int weight, LocalDate factUpdatedAt) {
}
