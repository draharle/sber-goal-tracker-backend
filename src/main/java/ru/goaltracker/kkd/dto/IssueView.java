package ru.goaltracker.kkd.dto;

/**
 * Проблема в списке GET /api/kkd/issues.
 *
 * @param id          стабильный между прогонами id (ISS-…), считается из fingerprint
 * @param deltaStatus new / persisting относительно прогона сравнения; null — сравнивать не с чем
 */
public record IssueView(String id, String fingerprint, String ruleCode, String ruleTitle, String severity,
                        String objectType, String objectId, String goalId, String goalTitle,
                        String krId, String krTitle, String unitId, String unitName,
                        String details, String actual, String expected, String deltaStatus) {
}
