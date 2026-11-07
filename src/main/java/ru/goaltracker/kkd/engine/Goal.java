package ru.goaltracker.kkd.engine;

import java.time.LocalDate;

/**
 * Цель сотрудника или подразделения.
 *
 * @param ownerId  ответственный сотрудник (может быть null)
 * @param parentId цель вышестоящего уровня, из которой каскадирована эта (может быть null)
 * @param level    уровень каскада: block → hq → unit → employee
 * @param status   draft / active / closed / cancelled
 */
public record Goal(String id, String title, String unitId, String ownerId, String parentId,
                   String level, String status, LocalDate deadline) {

    public static final String STATUS_ACTIVE = "active";
    public static final String LEVEL_BLOCK = "block";

    /** Правила ККД применяются только к активным целям и их КР. */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /** Цель уровня блока — вершина каскада, родителя у неё быть не должно. */
    public boolean isBlockLevel() {
        return LEVEL_BLOCK.equals(level);
    }
}
