package ru.goaltracker.kkd.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ответ GET /api/kkd/summary. Поля null означают «нет данных для сравнения» (первый прогон)
 * или «не из чего считать» (в подразделении нет активных целей).
 */
public record SummaryResponse(RunRef run, RunRef comparedTo, UnitView unit, List<MetricView> metrics,
                              List<SeverityCount> issuesBySeverity, Delta delta) {

    public record RunRef(String id, LocalDate snapshotDate) {
    }

    public record UnitView(String id, String name, String level, String parentId) {
    }

    public record MetricView(String key, String title, String unit, boolean higherIsBetter,
                             BigDecimal value, BigDecimal previous, BigDecimal delta, String trend) {
    }

    public record SeverityCount(String code, String title, int current, Integer previous, Integer delta) {
    }

    public record Delta(@JsonProperty("new") Integer newCount, Integer persisting, Integer resolved) {
    }
}
