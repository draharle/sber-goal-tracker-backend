package ru.goaltracker.kkd.dto;

import java.util.List;

/** Ответ GET /api/kkd/issues. */
public record IssuePage(String runId, List<IssueView> items, int total, int limit, int offset) {
}
