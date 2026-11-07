package ru.goaltracker.kkd.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ru.goaltracker.kkd.config.KkdProperties;
import ru.goaltracker.kkd.dto.PageResponse;
import ru.goaltracker.kkd.dto.RunView;
import ru.goaltracker.kkd.engine.DataSnapshot;
import ru.goaltracker.kkd.engine.Issue;
import ru.goaltracker.kkd.engine.Metric;
import ru.goaltracker.kkd.engine.MetricsCalculator;
import ru.goaltracker.kkd.engine.RuleEngine;
import ru.goaltracker.kkd.engine.UnitTree;
import ru.goaltracker.kkd.repository.GoalDataRepository;
import ru.goaltracker.kkd.repository.IssueRepository;
import ru.goaltracker.kkd.repository.RunMetricRepository;
import ru.goaltracker.kkd.repository.RunRecord;
import ru.goaltracker.kkd.repository.RunRepository;

/** Прогоны ККД: список, поиск нужного прогона, выполнение нового. */
@Service
public class RunService {

    public static final String LATEST = "latest";
    private static final Pattern RUN_ID = Pattern.compile("RUN-\\d{1,6}");
    private static final Logger log = LoggerFactory.getLogger(RunService.class);

    private final RunRepository runs;
    private final GoalDataRepository goalData;
    private final IssueRepository issues;
    private final RunMetricRepository metrics;
    private final RuleEngine engine;
    private final KkdProperties properties;
    private final TransactionTemplate tx;

    public RunService(RunRepository runs, GoalDataRepository goalData, IssueRepository issues,
                      RunMetricRepository metrics, RuleEngine engine, KkdProperties properties,
                      TransactionTemplate tx) {
        this.runs = runs;
        this.goalData = goalData;
        this.issues = issues;
        this.metrics = metrics;
        this.engine = engine;
        this.properties = properties;
        this.tx = tx;
    }

    public PageResponse<RunView> list(int limit, int offset) {
        List<RunView> items = runs.findPage(limit, offset).stream().map(RunView::from).toList();
        return new PageResponse<>(items, runs.count(), limit, offset);
    }

    /**
     * Прогон, результаты которого можно показать. run_id = latest (или пусто) — последний успешный.
     * Кривой формат — 422, нет такого — 404, упал — 409 RUN_FAILED, ещё идёт — 409 RUN_IN_PROGRESS.
     */
    public RunRecord findCompleted(String runIdParam) {
        String runId = runIdParam == null || runIdParam.isBlank() ? LATEST : runIdParam.trim();
        if (LATEST.equals(runId)) {
            return runs.findLatestCompleted().orElseThrow(() ->
                    new KkdException(ErrorCode.RUN_NOT_FOUND, "Нет ни одного успешного прогона"));
        }
        if (!RUN_ID.matcher(runId).matches()) {
            throw KkdException.invalidParam("run_id должен быть latest или вида RUN-001, получено: " + runId);
        }
        RunRecord run = runs.findById(runId).orElseThrow(() ->
                new KkdException(ErrorCode.RUN_NOT_FOUND, "Прогон " + runId + " не найден"));
        if (run.isFailed()) {
            throw new KkdException(ErrorCode.RUN_FAILED,
                    "Прогон " + runId + " завершился с ошибкой — результатов нет: " + run.error());
        }
        if (!run.isCompleted()) {
            throw new KkdException(ErrorCode.RUN_IN_PROGRESS, "Прогон " + runId + " ещё выполняется");
        }
        return run;
    }

    /**
     * Выполняет прогон: снимок данных → движок правил → метрики по всем подразделениям → запись в БД.
     * Сравнение — с последним успешным прогоном (упавшие пропускаются).
     * Если что-то сломалось посередине — прогон остаётся в истории со статусом failed и текстом ошибки.
     */
    public synchronized RunRecord execute(String triggerType) {
        if (runs.existsRunning()) {
            throw new KkdException(ErrorCode.RUN_IN_PROGRESS, "Прогон уже выполняется — дождитесь завершения");
        }
        LocalDate snapshotDate = properties.snapshotDate();
        String comparedTo = runs.findLatestCompleted().map(RunRecord::id).orElse(null);
        String runId = runs.createRunning(triggerType, snapshotDate, comparedTo, now());
        try {
            DataSnapshot snapshot = goalData.loadSnapshot(snapshotDate);
            List<Issue> found = engine.check(snapshot);
            Map<String, Map<Metric, BigDecimal>> unitMetrics =
                    MetricsCalculator.calculateForAllUnits(snapshot, found, new UnitTree(snapshot.units()));
            tx.executeWithoutResult(status -> {
                issues.saveAll(runId, found);
                metrics.saveAll(runId, unitMetrics);
                runs.markCompleted(runId, now());
            });
            log.info("Прогон {} на {} завершён: проблем {}, сравнение с {}", runId, snapshotDate, found.size(),
                    comparedTo);
        } catch (RuntimeException e) {
            log.error("Прогон {} завершился с ошибкой", runId, e);
            runs.markFailed(runId, "Ошибка выполнения прогона: " + e.getMessage(), now());
        }
        return runs.findById(runId).orElseThrow();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(properties.zone());
    }
}
