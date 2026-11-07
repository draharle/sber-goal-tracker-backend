package ru.goaltracker.kkd.config;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки модуля ККД (префикс kkd, значения — из переменных окружения, см. .env.example).
 *
 * @param today        дата снимка для прогонов в формате ГГГГ-ММ-ДД; пусто — текущая дата.
 *                     В демо фиксируем 2026-10-26, чтобы цифры не зависели от дня запуска
 * @param runOnStartup выполнить прогон при старте, если за дату снимка ещё нет успешного
 * @param zone         часовой пояс для «сегодня» и отметок времени прогона
 */
@ConfigurationProperties(prefix = "kkd")
public record KkdProperties(String today,
                            @DefaultValue("true") boolean runOnStartup,
                            @DefaultValue("Europe/Moscow") ZoneId zone) {

    public LocalDate snapshotDate() {
        return today == null || today.isBlank() ? LocalDate.now(zone) : LocalDate.parse(today.trim());
    }
}
