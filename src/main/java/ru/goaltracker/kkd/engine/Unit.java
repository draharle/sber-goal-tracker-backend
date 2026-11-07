package ru.goaltracker.kkd.engine;

/**
 * Подразделение оргструктуры. parentId = null — корень (блок).
 *
 * @param level block / hq / unit
 */
public record Unit(String id, String name, String level, String parentId) {
}
