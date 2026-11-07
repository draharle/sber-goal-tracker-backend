package ru.goaltracker.kkd.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Строка таблицы kkd_run плюс число найденных проблем (null, если прогон не завершился успешно).
 */
public record RunRecord(String id, int number, String status, String triggerType, LocalDate snapshotDate,
                        String comparedToRunId, OffsetDateTime startedAt, OffsetDateTime finishedAt,
                        String error, Integer issuesTotal) {

    public static final String RUNNING = "running";
    public static final String COMPLETED = "completed";
    public static final String FAILED = "failed";

    public boolean isCompleted() {
        return COMPLETED.equals(status);
    }

    public boolean isFailed() {
        return FAILED.equals(status);
    }

    public static String idOf(int number) {
        return String.format("RUN-%03d", number);
    }
}
