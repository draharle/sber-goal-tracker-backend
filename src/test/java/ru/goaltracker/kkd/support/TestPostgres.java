package ru.goaltracker.kkd.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Встроенный PostgreSQL 16 для тестов (бинарники zonky, Docker не нужен).
 * Сервер поднимается один раз на весь прогон тестов, каждому тесту — своя пустая база.
 */
public final class TestPostgres {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static EmbeddedPostgres server;

    private TestPostgres() {
    }

    /** Новая пустая база на общем сервере. */
    public static DataSource newDatabase(String prefix) {
        String name = prefix + "_" + COUNTER.incrementAndGet();
        try (Connection c = server().getPostgresDatabase().getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось создать тестовую базу " + name, e);
        }
        return server().getDatabase("postgres", name);
    }

    private static synchronized EmbeddedPostgres server() {
        if (server == null) {
            try {
                server = EmbeddedPostgres.builder()
                        .setLocaleConfig("locale", "C")
                        .setLocaleConfig("encoding", "UTF8")
                        .start();
            } catch (IOException e) {
                throw new UncheckedIOException("Встроенный PostgreSQL не запустился", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.close();
                } catch (IOException ignored) {
                    // при выходе из JVM ошибку закрытия игнорируем
                }
            }));
        }
        return server;
    }
}
