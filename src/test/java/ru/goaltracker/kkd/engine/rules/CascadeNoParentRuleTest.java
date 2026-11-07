package ru.goaltracker.kkd.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.goaltracker.kkd.engine.TestData.context;
import static ru.goaltracker.kkd.engine.TestData.goal;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.goaltracker.kkd.engine.Issue;

@DisplayName("CASCADE_NO_PARENT: цель ниже уровня блока должна быть привязана к вышестоящей")
class CascadeNoParentRuleTest {

    private final CascadeNoParentRule rule = new CascadeNoParentRule();

    @Test
    @DisplayName("цель блока без родителя — норма, цели hq / unit / employee без родителя — проблема")
    void blockIsTopOfCascade() {
        List<Issue> issues = rule.check(context(List.of(
                goal("G-BLOCK", "B2B", null, "block"),
                goal("G-HQ", "HQ", null, "hq"),
                goal("G-UNIT", "U1", null, "unit"),
                goal("G-EMP", "U1", null, "employee")), List.of()));

        assertThat(issues).extracting(Issue::objectId).containsExactlyInAnyOrder("G-HQ", "G-UNIT", "G-EMP");
    }

    @Test
    @DisplayName("привязанная цель — норма; ссылка на несуществующую цель — проблема")
    void linkedAndDanglingParent() {
        List<Issue> issues = rule.check(context(List.of(
                goal("G-BLOCK", "B2B", null, "block"),
                goal("G-OK", "HQ", "G-BLOCK", "hq"),
                goal("G-LOST", "HQ", "G-DELETED", "hq")), List.of()));

        assertThat(issues).singleElement().satisfies(i -> {
            assertThat(i.objectId()).isEqualTo("G-LOST");
            assertThat(i.actual()).isEqualTo("G-DELETED");
        });
    }
}
