package ru.goaltracker.kkd.engine;

/** Счётчики дельты; все null — сравнения нет (первый прогон). */
public record DeltaCounts(Integer newCount, Integer persisting, Integer resolved) {

    public static final DeltaCounts NO_COMPARISON = new DeltaCounts(null, null, null);
}
