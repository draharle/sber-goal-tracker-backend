package ru.goaltracker.kkd.repository;

import ru.goaltracker.kkd.engine.Issue;

/** Проблема из БД вместе с названиями для показа (правило, цель, КР, подразделение). */
public record IssueRow(Issue issue, String ruleTitle, String goalTitle, String krTitle, String unitName) {
}
