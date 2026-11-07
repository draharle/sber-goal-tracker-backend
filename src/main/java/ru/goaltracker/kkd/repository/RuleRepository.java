package ru.goaltracker.kkd.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Справочник правил kkd_rule. */
@Repository
public class RuleRepository {

    public record RuleRecord(String code, String title, String severity, String objectType,
                             String description, String recommendation) {
    }

    private final JdbcTemplate jdbc;

    public RuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RuleRecord> findAll() {
        return jdbc.query("""
                SELECT code, title, severity, object_type, description, recommendation
                FROM kkd_rule ORDER BY code
                """, (rs, n) -> new RuleRecord(rs.getString("code"), rs.getString("title"),
                rs.getString("severity"), rs.getString("object_type"), rs.getString("description"),
                rs.getString("recommendation")));
    }
}
