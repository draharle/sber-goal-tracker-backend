package ru.goaltracker.kkd.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.goaltracker.kkd.engine.Unit;
import ru.goaltracker.kkd.engine.UnitTree;

@Repository
public class UnitRepository {

    private final JdbcTemplate jdbc;

    public UnitRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Unit> findAll() {
        return jdbc.query("SELECT id, name, level, parent_id FROM unit ORDER BY id",
                (rs, n) -> new Unit(rs.getString("id"), rs.getString("name"),
                        rs.getString("level"), rs.getString("parent_id")));
    }

    /** Оргструктура целиком (таблица маленькая) — иерархию дальше обходит {@link UnitTree}. */
    public UnitTree loadTree() {
        return new UnitTree(findAll());
    }
}
