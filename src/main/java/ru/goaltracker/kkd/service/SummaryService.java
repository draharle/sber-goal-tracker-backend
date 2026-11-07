package ru.goaltracker.kkd.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import ru.goaltracker.kkd.dto.SummaryResponse;
import ru.goaltracker.kkd.dto.SummaryResponse.Delta;
import ru.goaltracker.kkd.dto.SummaryResponse.MetricView;
import ru.goaltracker.kkd.dto.SummaryResponse.RunRef;
import ru.goaltracker.kkd.dto.SummaryResponse.SeverityCount;
import ru.goaltracker.kkd.dto.SummaryResponse.UnitView;
import ru.goaltracker.kkd.engine.DeltaCalculator;
import ru.goaltracker.kkd.engine.DeltaCounts;
import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Metric;
import ru.goaltracker.kkd.engine.MetricComparison;
import ru.goaltracker.kkd.engine.Severity;
import ru.goaltracker.kkd.engine.Unit;
import ru.goaltracker.kkd.repository.IssueRepository;
import ru.goaltracker.kkd.repository.RunMetricRepository;
import ru.goaltracker.kkd.repository.RunRecord;
import ru.goaltracker.kkd.repository.RunRepository;
import ru.goaltracker.kkd.service.UnitScopeService.UnitScope;

/**
 * Сводка для страницы ККД: метрики подразделения (с учётом дочерних) против прогона сравнения,
 * разбивка проблем по важности и дельта new / persisting / resolved.
 */
@Service
public class SummaryService {

    private final RunService runService;
    private final RunRepository runs;
    private final UnitScopeService unitScopes;
    private final IssueRepository issues;
    private final RunMetricRepository metrics;

    public SummaryService(RunService runService, RunRepository runs, UnitScopeService unitScopes,
                          IssueRepository issues, RunMetricRepository metrics) {
        this.runService = runService;
        this.runs = runs;
        this.unitScopes = unitScopes;
        this.issues = issues;
        this.metrics = metrics;
    }

    public SummaryResponse summary(String runIdParam, String unitIdParam) {
        UnitScope scope = unitScopes.resolve(unitIdParam);
        RunRecord run = runService.findCompleted(runIdParam);
        RunRecord previous = previousOf(run);

        List<Issue> currentIssues = inScope(issues.findByRun(run.id()), scope.unitIds());
        List<Issue> previousIssues = previous == null ? null
                : inScope(issues.findByRun(previous.id()), scope.unitIds());

        Map<Metric, BigDecimal> currentMetrics = metrics.find(run.id(), scope.unit().id());
        Map<Metric, BigDecimal> previousMetrics = previous == null ? Map.of()
                : metrics.find(previous.id(), scope.unit().id());

        List<MetricView> metricViews = new ArrayList<>();
        for (Metric m : Metric.values()) {
            BigDecimal prev = previous == null ? null : previousMetrics.get(m);
            MetricComparison c = MetricComparison.of(currentMetrics.get(m), prev, m.higherIsBetter());
            metricViews.add(new MetricView(m.key(), m.title(), m.unit(), m.higherIsBetter(),
                    c.value(), c.previous(), c.delta(), c.trend() == null ? null : c.trend().code()));
        }

        Map<Severity, Integer> currentBySeverity = DeltaCalculator.countBySeverity(currentIssues);
        Map<Severity, Integer> previousBySeverity = previousIssues == null ? null
                : DeltaCalculator.countBySeverity(previousIssues);
        List<SeverityCount> bySeverity = new ArrayList<>();
        for (Severity s : Severity.values()) {
            int cur = currentBySeverity.get(s);
            Integer prev = previousBySeverity == null ? null : previousBySeverity.get(s);
            bySeverity.add(new SeverityCount(s.code(), s.title(), cur, prev, prev == null ? null : cur - prev));
        }

        DeltaCounts delta = DeltaCalculator.compare(currentIssues, previousIssues);
        Unit unit = scope.unit();
        return new SummaryResponse(
                new RunRef(run.id(), run.snapshotDate()),
                previous == null ? null : new RunRef(previous.id(), previous.snapshotDate()),
                new UnitView(unit.id(), unit.name(), unit.level(), unit.parentId()),
                metricViews,
                bySeverity,
                new Delta(delta.newCount(), delta.persisting(), delta.resolved()));
    }

    /** Прогон, с которым сравниваем: записан в прогоне при запуске (последний успешный на тот момент). */
    private RunRecord previousOf(RunRecord run) {
        if (run.comparedToRunId() == null) {
            return null;
        }
        return runs.findById(run.comparedToRunId()).filter(RunRecord::isCompleted).orElse(null);
    }

    private static List<Issue> inScope(List<Issue> all, Set<String> unitIds) {
        return all.stream().filter(i -> unitIds.contains(i.unitId())).toList();
    }
}
