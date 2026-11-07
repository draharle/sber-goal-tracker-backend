package ru.goaltracker.kkd.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.goaltracker.kkd.support.TestPostgres;

/**
 * Условие приёмки из задания: накат → откат → повторный накат без ошибок.
 * Миграции те же, что накатывает приложение при старте; откат — до тега baseline (пустая схема).
 */
@DisplayName("Миграции Liquibase: накат → откат → повторный накат")
class LiquibaseRollbackTest {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";
    private static final List<String> TABLES =
            List.of("unit", "employee", "goal", "key_result", "kkd_rule", "kkd_run", "kkd_issue", "kkd_run_metric");

    @Test
    void updateRollbackUpdate() throws Exception {
        DataSource dataSource = TestPostgres.newDatabase("kkd_migrations");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
            Contexts demo = new Contexts("demo");

            // 1. Накат: схема + справочник + демо-данные
            liquibase.update(demo, new LabelExpression());
            assertSeeded(jdbc);

            // Как в жизни: после старта приложение уже сделало свой прогон, который ссылается на демо-прогон
            jdbc.update("""
                    INSERT INTO kkd_run (id, number, status, trigger_type, snapshot_date, compared_to_run_id, started_at)
                    VALUES ('RUN-003', 3, 'completed', 'startup', '2026-10-26', 'RUN-001', now())""");
            jdbc.update("""
                    INSERT INTO kkd_issue (run_id, fingerprint, rule_code, severity, object_type, object_id,
                                           goal_id, unit_id, details)
                    VALUES ('RUN-003', 'GOAL_NO_KR:G-013', 'GOAL_NO_KR', 'critical', 'goal', 'G-013',
                            'G-013', 'CIB-KEY', 'test')""");

            // 2. Полный откат до тега baseline: таблиц не остаётся
            liquibase.rollback("baseline", demo, new LabelExpression());
            for (String table : TABLES) {
                assertThat(tableExists(jdbc, table)).as("таблица %s удалена", table).isFalse();
            }

            // 3. Повторный накат проходит без ошибок, данные снова на месте
            liquibase.update(demo, new LabelExpression());
            assertSeeded(jdbc);
            assertThat(count(jdbc, "SELECT COUNT(*) FROM kkd_run")).isEqualTo(2);
        }
    }

    private static void assertSeeded(JdbcTemplate jdbc) {
        for (String table : TABLES) {
            assertThat(tableExists(jdbc, table)).as("таблица %s есть", table).isTrue();
        }
        assertThat(count(jdbc, "SELECT COUNT(*) FROM unit")).isEqualTo(5);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM goal")).isEqualTo(26);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM key_result")).isEqualTo(40);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM kkd_rule")).isEqualTo(4);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM kkd_issue WHERE run_id = 'RUN-001'")).isEqualTo(12);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM kkd_run_metric WHERE run_id = 'RUN-001'")).isEqualTo(15);
    }

    private static boolean tableExists(JdbcTemplate jdbc, String table) {
        return count(jdbc, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' "
                + "AND table_name = '" + table + "'") > 0;
    }

    private static int count(JdbcTemplate jdbc, String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }
}
