package ru.goaltracker.kkd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.goaltracker.kkd.engine.RuleEngine;
import ru.goaltracker.kkd.engine.rules.StandardRules;

/** Движок — обычный Java-объект; Spring только создаёт его и отдаёт сервисам. */
@Configuration
public class EngineConfig {

    @Bean
    public RuleEngine ruleEngine() {
        return new RuleEngine(StandardRules.all());
    }
}
