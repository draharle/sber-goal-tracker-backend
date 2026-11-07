package ru.goaltracker.kkd.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Найденная проблема: какое правило нарушено, на каком объекте (цель или КР) и в каком подразделении.
 *
 * @param objectType goal / key_result
 * @param objectId   id цели или КР, на котором сработало правило
 * @param goalId     цель, к которой относится проблема (для КР — его цель)
 * @param krId       id КР или null для проблем уровня цели
 * @param unitId     подразделение цели на момент прогона
 * @param actual     фактическое значение (текстом, может быть null)
 * @param expected   ожидаемое значение (текстом)
 */
public record Issue(String ruleCode, Severity severity, String objectType, String objectId,
                    String goalId, String krId, String unitId, String details,
                    String actual, String expected) {

    public static final String OBJECT_GOAL = "goal";
    public static final String OBJECT_KEY_RESULT = "key_result";

    /** Проблема на уровне цели. */
    public static Issue forGoal(Rule rule, Goal goal, String details, String actual, String expected) {
        return new Issue(rule.code(), rule.severity(), OBJECT_GOAL, goal.id(), goal.id(), null,
                goal.unitId(), details, actual, expected);
    }

    /** Проблема на уровне ключевого результата. */
    public static Issue forKeyResult(Rule rule, Goal goal, KeyResult kr, String details,
                                     String actual, String expected) {
        return new Issue(rule.code(), rule.severity(), OBJECT_KEY_RESULT, kr.id(), goal.id(), kr.id(),
                goal.unitId(), details, actual, expected);
    }

    /**
     * «Отпечаток» проблемы: правило + объект. Две проблемы из разных прогонов — одна и та же,
     * если отпечатки совпадают. На этом строится дельта new / persisting / resolved.
     */
    public String fingerprint() {
        return fingerprint(ruleCode, objectId);
    }

    public static String fingerprint(String ruleCode, String objectId) {
        return ruleCode + ":" + objectId;
    }

    /** Стабильный между прогонами id проблемы для API: ISS- + первые 10 символов SHA-1 отпечатка. */
    public String publicId() {
        return publicId(fingerprint());
    }

    public static String publicId(String fingerprint) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            return "ISS-" + HexFormat.of().formatHex(hash).substring(0, 10).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }
}
