package ru.goaltracker.kkd.engine;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Уровень важности проблемы. Порядок объявления = порядок сортировки (critical первым). */
public enum Severity {
    CRITICAL("Критичная"),
    MAJOR("Существенная"),
    MINOR("Незначительная");

    private final String title;

    Severity(String title) {
        this.title = title;
    }

    /** Код в API и в БД: critical / major / minor. */
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String title() {
        return title;
    }

    public static Optional<Severity> fromCode(String code) {
        return Arrays.stream(values()).filter(s -> s.code().equals(code)).findFirst();
    }
}
