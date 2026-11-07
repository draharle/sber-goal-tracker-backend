package ru.goaltracker.kkd.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RunRepository {

    private static final String SELECT_RUN = """
            SELECT r.id, r.number, r.status, r.trigger_type, r.snapshot_date, r.compared_to_run_id,
                   r.started_at, r.finished_at, r.error,
                   CASE WHEN r.status = 'completed'
                        THEN (SELECT COUNT(*) FROM kkd_issue i WHERE i.run_id = r.id) END AS issues_total
            FROM kkd_run r
            """;

    private final JdbcTemplate jdbc;

    public RunRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Прогоны от новых к старым. */
    public List<RunRecord> findPage(int limit, int offset) {
        return jdbc.query(SELECT_RUN + " ORDER BY r.number DESC LIMIT ? OFFSET ?", RunRepository::map, limit, offset);
    }

    public int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM kkd_run", Integer.class);
        return n == null ? 0 : n;
    }

    public Optional<RunRecord> findById(String id) {
        return jdbc.query(SELECT_RUN + " WHERE r.id = ?", RunRepository::map, id).stream().findFirst();
    }

    /** Последний успешный прогон — его отдаёт run_id=latest и с ним сравнивается новый прогон. */
    public Optional<RunRecord> findLatestCompleted() {
        return jdbc.query(SELECT_RUN + " WHERE r.status = 'completed' ORDER BY r.number DESC LIMIT 1",
                RunRepository::map).stream().findFirst();
    }

    public boolean existsRunning() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM kkd_run WHERE status = 'running')", Boolean.class));
    }

    public boolean existsCompletedOnOrAfter(LocalDate snapshotDate) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM kkd_run WHERE status = 'completed' AND snapshot_date >= ?)",
                Boolean.class, snapshotDate));
    }

    /** Создаёт прогон в статусе running. Номер = максимальный + 1 (уникальность гарантирует UNIQUE). */
    public String createRunning(String triggerType, LocalDate snapshotDate, String comparedToRunId,
                                OffsetDateTime startedAt) {
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(number), 0) FROM kkd_run", Integer.class);
        int number = (max == null ? 0 : max) + 1;
        String id = RunRecord.idOf(number);
        jdbc.update("""
                INSERT INTO kkd_run (id, number, status, trigger_type, snapshot_date, compared_to_run_id, started_at)
                VALUES (?, ?, 'running', ?, ?, ?, ?)
                """, id, number, triggerType, snapshotDate, comparedToRunId, startedAt);
        return id;
    }

    public void markCompleted(String id, OffsetDateTime finishedAt) {
        jdbc.update("UPDATE kkd_run SET status = 'completed', finished_at = ? WHERE id = ?", finishedAt, id);
    }

    public void markFailed(String id, String error, OffsetDateTime finishedAt) {
        jdbc.update("UPDATE kkd_run SET status = 'failed', error = ?, finished_at = ? WHERE id = ?",
                error, finishedAt, id);
    }

    private static RunRecord map(ResultSet rs, int rowNum) throws SQLException {
        long issues = rs.getLong("issues_total");
        Integer issuesTotal = rs.wasNull() ? null : (int) issues;
        return new RunRecord(
                rs.getString("id"), rs.getInt("number"), rs.getString("status"), rs.getString("trigger_type"),
                rs.getObject("snapshot_date", LocalDate.class), rs.getString("compared_to_run_id"),
                rs.getObject("started_at", OffsetDateTime.class), rs.getObject("finished_at", OffsetDateTime.class),
                rs.getString("error"), issuesTotal);
    }
}
