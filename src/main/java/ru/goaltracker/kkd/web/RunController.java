package ru.goaltracker.kkd.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.goaltracker.kkd.dto.PageResponse;
import ru.goaltracker.kkd.dto.RunView;
import ru.goaltracker.kkd.service.RunService;

/** Прогоны ККД. Контроллер только принимает параметры и отдаёт ответ — логика в RunService. */
@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    /** Список прогонов от новых к старым: статус, дата снимка, с каким сравнивается, число проблем. */
    @GetMapping
    public PageResponse<RunView> list(
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "limit должен быть от 1 до 100")
            @Max(value = 100, message = "limit должен быть от 1 до 100") int limit,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "offset не может быть отрицательным") int offset) {
        return runService.list(limit, offset);
    }

    /** Внеплановый прогон: выполняется синхронно, в ответе — созданный прогон (completed или failed). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunView start() {
        return RunView.from(runService.execute("manual"));
    }
}
