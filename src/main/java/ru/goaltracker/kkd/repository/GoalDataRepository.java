package ru.goaltracker.kkd.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.goaltracker.kkd.engine.DataSnapshot;
import ru.goaltracker.kkd.engine.Goal;
import ru.goaltracker.kkd.engine.KeyResult;

/** Читает цели и КР и собирает снимок данных для движка правил. */
@Repository
public class GoalDataRepository {

    private final JdbcTemplate jdbc;
    private final UnitRepository units;

    public GoalDataRepository(JdbcTemplate jdbc, UnitRepository units) {
        this.jdbc = jdbc;
        this.units = units;
    }

    public DataSnapshot loadSnapshot(LocalDate snapshotDate) {
        return new DataSnapshot(snapshotDate, units.findAll(), findGoals(), findKeyResults());
    }

    public List<Goal> findGoals() {
        return jdbc.query("""
                SELECT id, title, unit_id, owner_id, parent_id, level, status, deadline
                FROM goal ORDER BY id
                """, (rs, n) -> new Goal(
                rs.getString("id"), rs.getString("title"), rs.getString("unit_id"),
                rs.getString("owner_id"), rs.getString("parent_id"), rs.getString("level"),
                rs.getString("status"), rs.getObject("deadline", LocalDate.class)));
    }

    public List<KeyResult> findKeyResults() {
        return jdbc.query("""
                SELECT id, goal_id, title, metric_unit, target_value, fact_value, weight, fact_updated_at
                FROM key_result ORDER BY id
                """, (rs, n) -> new KeyResult(
                rs.getString("id"), rs.getString("goal_id"), rs.getString("title"),
                rs.getString("metric_unit"), rs.getBigDecimal("target_value"), rs.getBigDecimal("fact_value"),
                rs.getInt("weight"), rs.getObject("fact_updated_at", LocalDate.class)));
    }
}
