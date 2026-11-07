package ru.goaltracker.kkd.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Оргструктура как дерево. Главное — {@link #descendantsOrSelf(String)}: подразделение
 * и все его дочерние на любой глубине. Так метрики и фильтры учитывают каскад.
 */
public final class UnitTree {

    private final Map<String, Unit> byId = new LinkedHashMap<>();
    private final Map<String, List<Unit>> children = new LinkedHashMap<>();

    public UnitTree(Collection<Unit> units) {
        units.stream().sorted(Comparator.comparing(Unit::id)).forEach(u -> {
            byId.put(u.id(), u);
            if (u.parentId() != null) {
                children.computeIfAbsent(u.parentId(), k -> new ArrayList<>()).add(u);
            }
        });
    }

    public Optional<Unit> find(String unitId) {
        return Optional.ofNullable(byId.get(unitId));
    }

    public boolean contains(String unitId) {
        return byId.containsKey(unitId);
    }

    public Collection<Unit> units() {
        return byId.values();
    }

    /** Корневое подразделение (без родителя); если корней несколько — первый по id. */
    public Optional<Unit> root() {
        return byId.values().stream().filter(u -> u.parentId() == null).findFirst();
    }

    /** Подразделение и все его потомки (рекурсивный обход вниз по дереву). */
    public Set<String> descendantsOrSelf(String unitId) {
        Set<String> result = new LinkedHashSet<>();
        if (byId.containsKey(unitId)) {
            collect(unitId, result);
        }
        return result;
    }

    private void collect(String unitId, Set<String> acc) {
        if (!acc.add(unitId)) {
            return; // защита от цикла в кривых данных
        }
        for (Unit child : children.getOrDefault(unitId, List.of())) {
            collect(child.id(), acc);
        }
    }
}
