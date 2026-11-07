package ru.goaltracker.kkd.service;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ru.goaltracker.kkd.engine.Unit;
import ru.goaltracker.kkd.engine.UnitTree;
import ru.goaltracker.kkd.repository.UnitRepository;

/** Разбирает параметр unit_id и возвращает подразделение вместе со всеми дочерними. */
@Service
public class UnitScopeService {

    private static final Pattern UNIT_ID = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    /** Подразделение и множество id «оно + все дочерние» — по нему фильтруются проблемы. */
    public record UnitScope(Unit unit, Set<String> unitIds) {
    }

    private final UnitRepository units;

    public UnitScopeService(UnitRepository units) {
        this.units = units;
    }

    /** unit_id не задан — корень оргструктуры. Кривой формат — 422, неизвестный — 404. */
    public UnitScope resolve(String unitIdParam) {
        UnitTree tree = units.loadTree();
        Unit unit;
        if (unitIdParam == null || unitIdParam.isBlank()) {
            unit = tree.root().orElseThrow(() ->
                    new KkdException(ErrorCode.UNIT_NOT_FOUND, "Оргструктура пуста"));
        } else {
            String unitId = unitIdParam.trim();
            if (!UNIT_ID.matcher(unitId).matches()) {
                throw KkdException.invalidParam("unit_id может содержать только латиницу, цифры, '-' и '_' (до 32 символов)");
            }
            unit = tree.find(unitId).orElseThrow(() ->
                    new KkdException(ErrorCode.UNIT_NOT_FOUND, "Подразделение " + unitId + " не найдено"));
        }
        return new UnitScope(unit, tree.descendantsOrSelf(unit.id()));
    }
}
