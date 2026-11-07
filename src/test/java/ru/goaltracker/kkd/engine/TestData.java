package ru.goaltracker.kkd.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Конструктор маленьких наборов данных для тестов движка. */
public final class TestData {

    public static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 10, 26);

    /** Блок → департамент → два управления + пустой отдел. */
    public static final List<Unit> UNITS = List.of(
            new Unit("B2B", "Корпоративный блок", "block", null),
            new Unit("HQ", "Департамент", "hq", "B2B"),
            new Unit("U1", "Управление 1", "unit", "HQ"),
            new Unit("U2", "Управление 2", "unit", "HQ"),
            new Unit("EMPTY", "Пустой отдел", "unit", "HQ"));

    private TestData() {
    }

    public static Goal goal(String id, String unitId, String parentId, String level) {
        return goal(id, unitId, parentId, level, "active");
    }

    public static Goal goal(String id, String unitId, String parentId, String level, String status) {
        return new Goal(id, "Цель " + id, unitId, "E-1", parentId, level, status, LocalDate.of(2026, 12, 31));
    }

    public static KeyResult kr(String id, String goalId, int weight) {
        return kr(id, goalId, weight, SNAPSHOT_DATE.minusDays(1));
    }

    public static KeyResult kr(String id, String goalId, int weight, LocalDate factUpdatedAt) {
        return new KeyResult(id, goalId, "КР " + id, "шт.", BigDecimal.TEN, BigDecimal.ONE, weight, factUpdatedAt);
    }

    public static DataSnapshot snapshot(List<Goal> goals, List<KeyResult> keyResults) {
        return new DataSnapshot(SNAPSHOT_DATE, UNITS, goals, keyResults);
    }

    public static RuleContext context(List<Goal> goals, List<KeyResult> keyResults) {
        return new RuleContext(snapshot(goals, keyResults));
    }
}
