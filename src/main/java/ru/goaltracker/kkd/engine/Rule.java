package ru.goaltracker.kkd.engine;

import java.util.List;

/**
 * Правило ККД. Каждое правило — отдельный класс в пакете {@code engine.rules}.
 * Правило получает подготовленные данные (контекст) и возвращает список найденных проблем.
 * Оно ничего не знает про HTTP, БД и Spring — поэтому тестируется обычным JUnit без окружения.
 */
public interface Rule {

    /** Код правила, например GOAL_NO_KR. Совпадает с kkd_rule.code в БД. */
    String code();

    Severity severity();

    /** На каком объекте срабатывает: goal или key_result. */
    String objectType();

    List<Issue> check(RuleContext context);
}
