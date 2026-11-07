package ru.goaltracker.kkd.engine;

import java.time.LocalDate;
import java.util.List;

/**
 * Снимок данных, по которому выполняется прогон ККД: оргструктура, цели (все статусы) и КР
 * на дату snapshotDate. Движок получает его целиком и ничего не читает сам.
 */
public record DataSnapshot(LocalDate snapshotDate, List<Unit> units, List<Goal> goals,
                           List<KeyResult> keyResults) {

    public DataSnapshot {
        units = List.copyOf(units);
        goals = List.copyOf(goals);
        keyResults = List.copyOf(keyResults);
    }
}
