package ru.goaltracker.kkd.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ru.goaltracker.kkd.config.KkdProperties;
import ru.goaltracker.kkd.repository.RunRecord;
import ru.goaltracker.kkd.repository.RunRepository;

/**
 * При старте приложения выполняет прогон ККД, если за дату снимка (kkd.today) успешного прогона ещё нет.
 * В демо так появляется RUN-003: его считает движок по стартовым данным. Повторный старт ничего не дублирует.
 * Выключается переменной KKD_RUN_ON_STARTUP=false.
 */
@Component
@ConditionalOnProperty(prefix = "kkd", name = "run-on-startup", havingValue = "true", matchIfMissing = true)
public class StartupRunInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRunInitializer.class);

    private final RunService runService;
    private final RunRepository runs;
    private final KkdProperties properties;

    public StartupRunInitializer(RunService runService, RunRepository runs, KkdProperties properties) {
        this.runService = runService;
        this.runs = runs;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDate snapshotDate = properties.snapshotDate();
        if (runs.existsCompletedOnOrAfter(snapshotDate)) {
            log.info("Успешный прогон на {} уже есть — стартовый прогон не нужен", snapshotDate);
            return;
        }
        RunRecord run = runService.execute("startup");
        log.info("Стартовый прогон {}: статус {}, проблем {}", run.id(), run.status(), run.issuesTotal());
    }
}
