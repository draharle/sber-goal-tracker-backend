package ru.goaltracker.kkd.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ru.goaltracker.kkd.dto.IssuePage;
import ru.goaltracker.kkd.dto.IssueView;
import ru.goaltracker.kkd.engine.DeltaCalculator;
import ru.goaltracker.kkd.engine.DeltaStatus;
import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Severity;
import ru.goaltracker.kkd.repository.IssueRepository;
import ru.goaltracker.kkd.repository.IssueRow;
import ru.goaltracker.kkd.repository.RuleRepository;
import ru.goaltracker.kkd.repository.RunRecord;
import ru.goaltracker.kkd.service.UnitScopeService.UnitScope;

/** Список проблем прогона с фильтрами (подразделение с дочерними, правило, важность) и пагинацией. */
@Service
public class IssueQueryService {

    private final RunService runService;
    private final UnitScopeService unitScopes;
    private final IssueRepository issues;
    private final RuleRepository rules;

    public IssueQueryService(RunService runService, UnitScopeService unitScopes, IssueRepository issues,
                             RuleRepository rules) {
        this.runService = runService;
        this.unitScopes = unitScopes;
        this.issues = issues;
        this.rules = rules;
    }

    /**
     * @param ruleCodeCsv правила через запятую (GOAL_NO_KR,KR_WEIGHTS_SUM) или null
     * @param severityCsv важности через запятую (critical,major) или null
     */
    public IssuePage find(String runIdParam, String unitIdParam, String ruleCodeCsv, String severityCsv,
                          int limit, int offset) {
        Set<String> ruleCodes = parseRuleCodes(ruleCodeCsv);
        Set<String> severities = parseSeverities(severityCsv);
        UnitScope scope = unitScopes.resolve(unitIdParam);
        RunRecord run = runService.findCompleted(runIdParam);

        int total = issues.count(run.id(), scope.unitIds(), ruleCodes, severities);
        List<IssueRow> rows = issues.findPage(run.id(), scope.unitIds(), ruleCodes, severities, limit, offset);
        Set<String> previousFingerprints = run.comparedToRunId() == null ? null
                : DeltaCalculator.fingerprints(issues.findByRun(run.comparedToRunId()));

        List<IssueView> items = rows.stream().map(row -> toView(row, previousFingerprints)).toList();
        return new IssuePage(run.id(), items, total, limit, offset);
    }

    private Set<String> parseRuleCodes(String csv) {
        Set<String> values = csv(csv);
        Set<String> known = rules.findAll().stream().map(RuleRepository.RuleRecord::code).collect(Collectors.toSet());
        Set<String> unknown = values.stream().filter(v -> !known.contains(v)).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!unknown.isEmpty()) {
            throw KkdException.invalidParam("Неизвестные значения rule_code: " + String.join(", ", unknown)
                    + ". Допустимо: " + String.join(", ", known.stream().sorted().toList()));
        }
        return values;
    }

    private static Set<String> parseSeverities(String csv) {
        Set<String> values = csv(csv);
        Set<String> unknown = values.stream().filter(v -> Severity.fromCode(v).isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!unknown.isEmpty()) {
            throw KkdException.invalidParam("Неизвестные значения severity: " + String.join(", ", unknown)
                    + ". Допустимо: critical, major, minor");
        }
        return values;
    }

    private static Set<String> csv(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static IssueView toView(IssueRow row, Set<String> previousFingerprints) {
        Issue i = row.issue();
        DeltaStatus status = DeltaCalculator.statusOf(i.fingerprint(), previousFingerprints);
        return new IssueView(i.publicId(), i.fingerprint(), i.ruleCode(), row.ruleTitle(), i.severity().code(),
                i.objectType(), i.objectId(), i.goalId(), row.goalTitle(), i.krId(), row.krTitle(),
                i.unitId(), row.unitName(), i.details(), i.actual(), i.expected(),
                status == null ? null : status.code());
    }
}
