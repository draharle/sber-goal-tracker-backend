package ru.goaltracker.kkd.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Полный контекст приложения на встроенном PostgreSQL: миграции, демо-данные и стартовый прогон RUN-003
 * на дату 2026-10-26. HTTP-сервер не поднимается — запросы идут через MockMvc.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {"kkd.today=2026-10-26", "kkd.run-on-startup=true"})
@AutoConfigureMockMvc
@Import(EmbeddedPostgresConfig.class)
public @interface KkdIntegrationTest {
}
