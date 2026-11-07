package ru.goaltracker.kkd.engine;

import java.util.Arrays;
import java.util.Optional;

/** Метрики ККД. higherIsBetter нужен, чтобы по знаку дельты понять «лучше» или «хуже». */
public enum Metric {
    CLEAN_GOALS_SHARE("clean_goals_share", "Доля корректных целей", "%", true,
            "активные цели без проблем ÷ все активные цели × 100"),
    CASCADE_COVERAGE("cascade_coverage", "Покрытие каскадом", "%", true,
            "активные цели ниже уровня блока, привязанные к вышестоящей ÷ все такие цели × 100"),
    ISSUES_TOTAL("issues_total", "Найдено проблем", "шт.", false,
            "количество нарушений правил ККД");

    private final String key;
    private final String title;
    private final String unit;
    private final boolean higherIsBetter;
    private final String formula;

    Metric(String key, String title, String unit, boolean higherIsBetter, String formula) {
        this.key = key;
        this.title = title;
        this.unit = unit;
        this.higherIsBetter = higherIsBetter;
        this.formula = formula;
    }

    public String key() {
        return key;
    }

    public String title() {
        return title;
    }

    public String unit() {
        return unit;
    }

    public boolean higherIsBetter() {
        return higherIsBetter;
    }

    public String formula() {
        return formula;
    }

    public static Optional<Metric> fromKey(String key) {
        return Arrays.stream(values()).filter(m -> m.key.equals(key)).findFirst();
    }
}
