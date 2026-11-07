package ru.goaltracker.kkd.support;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Подменяет подключение к БД на встроенный PostgreSQL. Liquibase накатывает миграции как в бою. */
@TestConfiguration(proxyBeanMethods = false)
public class EmbeddedPostgresConfig {

    @Bean
    public DataSource dataSource() {
        return TestPostgres.newDatabase("kkd");
    }
}
