--liquibase formatted sql

--changeset kkd:002-kkd-rule
--comment: Справочник правил ККД
CREATE TABLE kkd_rule (
    code           VARCHAR(64)  PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    severity       VARCHAR(16)  NOT NULL CHECK (severity IN ('critical', 'major', 'minor')),
    object_type    VARCHAR(16)  NOT NULL CHECK (object_type IN ('goal', 'key_result')),
    description    TEXT         NOT NULL,
    recommendation TEXT
);
--rollback DROP TABLE kkd_rule;

--changeset kkd:002-kkd-run
--comment: Прогон ККД. compared_to_run_id — предыдущий успешный прогон на момент запуска
CREATE TABLE kkd_run (
    id                 VARCHAR(16) PRIMARY KEY,
    number             INTEGER     NOT NULL UNIQUE,
    status             VARCHAR(16) NOT NULL CHECK (status IN ('running', 'completed', 'failed')),
    trigger_type       VARCHAR(16) NOT NULL CHECK (trigger_type IN ('schedule', 'manual', 'startup')),
    snapshot_date      DATE        NOT NULL,
    compared_to_run_id VARCHAR(16) REFERENCES kkd_run (id),
    started_at         TIMESTAMPTZ NOT NULL,
    finished_at        TIMESTAMPTZ,
    error              TEXT
);
--rollback DROP TABLE kkd_run;

--changeset kkd:002-kkd-issue
--comment: Найденная проблема. Снимок на момент прогона: без внешних ключей на цели и подразделения, чтобы история не ломалась при изменении данных
CREATE TABLE kkd_issue (
    id             BIGSERIAL    PRIMARY KEY,
    run_id         VARCHAR(16)  NOT NULL REFERENCES kkd_run (id) ON DELETE CASCADE,
    fingerprint    VARCHAR(128) NOT NULL,
    rule_code      VARCHAR(64)  NOT NULL REFERENCES kkd_rule (code),
    severity       VARCHAR(16)  NOT NULL CHECK (severity IN ('critical', 'major', 'minor')),
    object_type    VARCHAR(16)  NOT NULL CHECK (object_type IN ('goal', 'key_result')),
    object_id      VARCHAR(32)  NOT NULL,
    goal_id        VARCHAR(32)  NOT NULL,
    kr_id          VARCHAR(32),
    unit_id        VARCHAR(32)  NOT NULL,
    details        TEXT         NOT NULL,
    actual_value   VARCHAR(255),
    expected_value VARCHAR(255),
    CONSTRAINT uq_kkd_issue_run_fingerprint UNIQUE (run_id, fingerprint)
);
CREATE INDEX ix_kkd_issue_run_unit ON kkd_issue (run_id, unit_id);
--rollback DROP TABLE kkd_issue;

--changeset kkd:002-kkd-run-metric
--comment: Значения метрик прогона по каждому подразделению (с учётом дочерних). Для дельты метрик не нужен старый снимок данных
CREATE TABLE kkd_run_metric (
    run_id     VARCHAR(16) NOT NULL REFERENCES kkd_run (id) ON DELETE CASCADE,
    unit_id    VARCHAR(32) NOT NULL,
    metric_key VARCHAR(64) NOT NULL,
    value      NUMERIC,
    PRIMARY KEY (run_id, unit_id, metric_key)
);
--rollback DROP TABLE kkd_run_metric;
