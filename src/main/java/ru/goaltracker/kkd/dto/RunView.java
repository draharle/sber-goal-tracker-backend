package ru.goaltracker.kkd.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import ru.goaltracker.kkd.repository.RunRecord;

/** Прогон в списке GET /api/runs и в ответе POST /api/runs. */
public record RunView(String id, int number, String status, String trigger, LocalDate snapshotDate,
                      OffsetDateTime startedAt, OffsetDateTime finishedAt, String comparedToRunId,
                      Integer issuesTotal, String error) {

    public static RunView from(RunRecord r) {
        return new RunView(r.id(), r.number(), r.status(), r.triggerType(), r.snapshotDate(), r.startedAt(),
                r.finishedAt(), r.comparedToRunId(), r.issuesTotal(), r.error());
    }
}
