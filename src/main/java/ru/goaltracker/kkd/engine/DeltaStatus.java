package ru.goaltracker.kkd.engine;

import java.util.Locale;

/** Статус проблемы относительно прошлого прогона. */
public enum DeltaStatus {
    NEW, PERSISTING, RESOLVED;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
