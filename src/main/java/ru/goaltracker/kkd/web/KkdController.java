package ru.goaltracker.kkd.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.goaltracker.kkd.dto.IssuePage;
import ru.goaltracker.kkd.dto.SummaryResponse;
import ru.goaltracker.kkd.service.IssueQueryService;
import ru.goaltracker.kkd.service.SummaryService;

/** Результаты ККД для страницы модуля: сводка и список проблем. */
@RestController
@RequestMapping("/api/kkd")
public class KkdController {

    private final SummaryService summaryService;
    private final IssueQueryService issueQueryService;

    public KkdController(SummaryService summaryService, IssueQueryService issueQueryService) {
        this.summaryService = summaryService;
        this.issueQueryService = issueQueryService;
    }

    /** run_id: latest (по умолчанию) или RUN-NNN; unit_id: по умолчанию корень оргструктуры. */
    @GetMapping("/summary")
    public SummaryResponse summary(@RequestParam(name = "run_id", required = false) String runId,
                                   @RequestParam(name = "unit_id", required = false) String unitId) {
        return summaryService.summary(runId, unitId);
    }

    @GetMapping("/issues")
    public IssuePage issues(
            @RequestParam(name = "run_id", required = false) String runId,
            @RequestParam(name = "unit_id", required = false) String unitId,
            @RequestParam(name = "rule_code", required = false) String ruleCode,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "limit должен быть от 1 до 200")
            @Max(value = 200, message = "limit должен быть от 1 до 200") int limit,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "offset не может быть отрицательным") int offset) {
        return issueQueryService.find(runId, unitId, ruleCode, severity, limit, offset);
    }
}
