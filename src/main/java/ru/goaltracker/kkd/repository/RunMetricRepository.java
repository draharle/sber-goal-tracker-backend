package ru.goaltracker.kkd.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.goaltracker.kkd.engine.Metric;

/**
 * Значения метрик прогона по подразделениям. Храним готовые числа, чтобы дельта метрик
 * не требовала заново собирать старый снимок данных (данные с тех пор могли измениться).
 */
@Repository
public class RunMetricRepository {

    private final JdbcTemplate jdbc;

    public RunMetricRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void saveAll(String runId, Map<String, Map<Metric, BigDecimal>> metricsByUnit) {
        List<Object[]> rows = new ArrayList<>();
        metricsByUnit.forEach((unitId, metrics) -> metrics.forEach(
                (metric, value) -> rows.add(new Object[] {runId, unitId, metric.key(), value})));
        jdbc.batchUpdate("INSERT INTO kkd_run_metric (run_id, unit_id, metric_key, value) VALUES (?, ?, ?, ?)",
                rows);
    }

    /** Метрики всех подразделений прогона: unit_id → (метрика → значение, может быть null). */
    public Map<String, Map<Metric, BigDecimal>> findAllByRun(String runId) {
        Map<String, Map<Metric, BigDecimal>> result = new LinkedHashMap<>();
        jdbc.query("SELECT unit_id, metric_key, value FROM kkd_run_metric WHERE run_id = ? ORDER BY unit_id",
                rs -> {
                    Map<Metric, BigDecimal> unitMetrics =
                            result.computeIfAbsent(rs.getString("unit_id"), k -> new EnumMap<>(Metric.class));
                    BigDecimal value = rs.getBigDecimal("value");
                    Metric.fromKey(rs.getString("metric_key")).ifPresent(m -> unitMetrics.put(m, value));
                }, runId);
        return result;
    }

    /** Метрики одного подразделения; пустая карта — подразделения в этом прогоне не было. */
    public Map<Metric, BigDecimal> find(String runId, String unitId) {
        Map<Metric, BigDecimal> result = new EnumMap<>(Metric.class);
        jdbc.query("SELECT metric_key, value FROM kkd_run_metric WHERE run_id = ? AND unit_id = ?",
                rs -> {
                    BigDecimal value = rs.getBigDecimal("value");
                    Metric.fromKey(rs.getString("metric_key")).ifPresent(m -> result.put(m, value));
                }, runId, unitId);
        return result;
    }
}
