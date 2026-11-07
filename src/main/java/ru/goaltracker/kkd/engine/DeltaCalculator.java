package ru.goaltracker.kkd.engine;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Дельта между прогонами. Проблема «та же самая», если совпадает отпечаток (правило + объект).
 * <ul>
 *   <li>new — есть сейчас, не было в прошлом прогоне;</li>
 *   <li>persisting — есть и сейчас, и в прошлом;</li>
 *   <li>resolved — была в прошлом, сейчас нет.</li>
 * </ul>
 * Прошлого прогона нет (previous = null) — сравнивать не с чем, все счётчики null, а не 0.
 */
public final class DeltaCalculator {

    private DeltaCalculator() {
    }

    public static DeltaCounts compare(Collection<Issue> current, Collection<Issue> previous) {
        if (previous == null) {
            return DeltaCounts.NO_COMPARISON;
        }
        Set<String> currentFps = fingerprints(current);
        Set<String> previousFps = fingerprints(previous);
        int persisting = (int) currentFps.stream().filter(previousFps::contains).count();
        int added = currentFps.size() - persisting;
        int resolved = (int) previousFps.stream().filter(fp -> !currentFps.contains(fp)).count();
        return new DeltaCounts(added, persisting, resolved);
    }

    /** Статус одной проблемы текущего прогона; null — если сравнивать не с чем. */
    public static DeltaStatus statusOf(String fingerprint, Set<String> previousFingerprints) {
        if (previousFingerprints == null) {
            return null;
        }
        return previousFingerprints.contains(fingerprint) ? DeltaStatus.PERSISTING : DeltaStatus.NEW;
    }

    public static Set<String> fingerprints(Collection<Issue> issues) {
        return issues.stream().map(Issue::fingerprint).collect(Collectors.toSet());
    }

    /** Сколько проблем каждой важности (все уровни присутствуют, даже если 0). */
    public static Map<Severity, Integer> countBySeverity(List<Issue> issues) {
        Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
        for (Severity s : Severity.values()) {
            counts.put(s, 0);
        }
        issues.forEach(i -> counts.merge(i.severity(), 1, Integer::sum));
        return counts;
    }
}
