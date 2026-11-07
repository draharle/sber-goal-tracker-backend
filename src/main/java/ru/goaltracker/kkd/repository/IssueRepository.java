package ru.goaltracker.kkd.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Severity;

@Repository
public class IssueRepository {

    private static final String ISSUE_COLUMNS = """
            i.rule_code, i.severity, i.object_type, i.object_id, i.goal_id, i.kr_id, i.unit_id,
            i.details, i.actual_value, i.expected_value
            """;
    private static final String ORDER = """
            ORDER BY CASE i.severity WHEN 'critical' THEN 0 WHEN 'major' THEN 1 ELSE 2 END,
                     i.unit_id, i.object_id, i.rule_code
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public IssueRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    public void saveAll(String runId, List<Issue> issues) {
        jdbc.batchUpdate("""
                INSERT INTO kkd_issue (run_id, fingerprint, rule_code, severity, object_type, object_id,
                                       goal_id, kr_id, unit_id, details, actual_value, expected_value)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, issues, 500, (ps, i) -> {
            ps.setString(1, runId);
            ps.setString(2, i.fingerprint());
            ps.setString(3, i.ruleCode());
            ps.setString(4, i.severity().code());
            ps.setString(5, i.objectType());
            ps.setString(6, i.objectId());
            ps.setString(7, i.goalId());
            ps.setString(8, i.krId());
            ps.setString(9, i.unitId());
            ps.setString(10, i.details());
            ps.setString(11, i.actual());
            ps.setString(12, i.expected());
        });
    }

    /** Все проблемы прогона — в тех же объектах, что возвращает движок. */
    public List<Issue> findByRun(String runId) {
        return jdbc.query("SELECT " + ISSUE_COLUMNS + " FROM kkd_issue i WHERE i.run_id = ? " + ORDER,
                IssueRepository::mapIssue, runId);
    }

    public int count(String runId, Collection<String> unitIds, Collection<String> ruleCodes,
                     Collection<String> severities) {
        MapSqlParameterSource params = params(runId, unitIds, ruleCodes, severities);
        Integer n = named.queryForObject("SELECT COUNT(*) FROM kkd_issue i " + where(ruleCodes, severities),
                params, Integer.class);
        return n == null ? 0 : n;
    }

    public List<IssueRow> findPage(String runId, Collection<String> unitIds, Collection<String> ruleCodes,
                                   Collection<String> severities, int limit, int offset) {
        MapSqlParameterSource params = params(runId, unitIds, ruleCodes, severities)
                .addValue("limit", limit)
                .addValue("offset", offset);
        String sql = "SELECT " + ISSUE_COLUMNS + """
                , r.title AS rule_title, g.title AS goal_title, k.title AS kr_title, u.name AS unit_name
                FROM kkd_issue i
                JOIN kkd_rule r ON r.code = i.rule_code
                LEFT JOIN goal g ON g.id = i.goal_id
                LEFT JOIN key_result k ON k.id = i.kr_id
                LEFT JOIN unit u ON u.id = i.unit_id
                """ + where(ruleCodes, severities) + ORDER + " LIMIT :limit OFFSET :offset";
        return named.query(sql, params, (rs, n) -> new IssueRow(mapIssue(rs, n),
                rs.getString("rule_title"), rs.getString("goal_title"), rs.getString("kr_title"),
                rs.getString("unit_name")));
    }

    private static String where(Collection<String> ruleCodes, Collection<String> severities) {
        StringBuilder sql = new StringBuilder(" WHERE i.run_id = :runId AND i.unit_id IN (:unitIds)");
        if (!ruleCodes.isEmpty()) {
            sql.append(" AND i.rule_code IN (:ruleCodes)");
        }
        if (!severities.isEmpty()) {
            sql.append(" AND i.severity IN (:severities)");
        }
        return sql.append(' ').toString();
    }

    private static MapSqlParameterSource params(String runId, Collection<String> unitIds,
                                                Collection<String> ruleCodes, Collection<String> severities) {
        return new MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("unitIds", unitIds)
                .addValue("ruleCodes", ruleCodes)
                .addValue("severities", severities);
    }

    private static Issue mapIssue(ResultSet rs, int rowNum) throws SQLException {
        Severity severity = Severity.fromCode(rs.getString("severity"))
                .orElseThrow(() -> new IllegalStateException("Unknown severity in kkd_issue"));
        return new Issue(rs.getString("rule_code"), severity, rs.getString("object_type"),
                rs.getString("object_id"), rs.getString("goal_id"), rs.getString("kr_id"),
                rs.getString("unit_id"), rs.getString("details"), rs.getString("actual_value"),
                rs.getString("expected_value"));
    }
}
