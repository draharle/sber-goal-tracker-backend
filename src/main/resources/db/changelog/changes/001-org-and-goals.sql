--liquibase formatted sql

--changeset kkd:001-unit
--comment: Подразделения с иерархией: parent_id ссылается на вышестоящее подразделение
CREATE TABLE unit (
    id        VARCHAR(32)  PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    level     VARCHAR(16)  NOT NULL CHECK (level IN ('block', 'hq', 'unit')),
    parent_id VARCHAR(32)  REFERENCES unit (id),
    CONSTRAINT ck_unit_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX ix_unit_parent ON unit (parent_id);
--rollback DROP TABLE unit;

--changeset kkd:001-employee
--comment: Сотрудники — ответственные за цели
CREATE TABLE employee (
    id        VARCHAR(32)  PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    position  VARCHAR(255),
    unit_id   VARCHAR(32)  NOT NULL REFERENCES unit (id)
);
--rollback DROP TABLE employee;

--changeset kkd:001-goal
--comment: Цели. parent_id — цель вышестоящего уровня (каскад), level — уровень каскада
CREATE TABLE goal (
    id        VARCHAR(32)  PRIMARY KEY,
    title     VARCHAR(500) NOT NULL,
    unit_id   VARCHAR(32)  NOT NULL REFERENCES unit (id),
    owner_id  VARCHAR(32)  REFERENCES employee (id),
    parent_id VARCHAR(32)  REFERENCES goal (id),
    level     VARCHAR(16)  NOT NULL CHECK (level IN ('block', 'hq', 'unit', 'employee')),
    status    VARCHAR(16)  NOT NULL CHECK (status IN ('draft', 'active', 'closed', 'cancelled')),
    deadline  DATE         NOT NULL,
    CONSTRAINT ck_goal_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX ix_goal_unit ON goal (unit_id);
CREATE INDEX ix_goal_parent ON goal (parent_id);
--rollback DROP TABLE goal;

--changeset kkd:001-key-result
--comment: Ключевые результаты цели: целевое и фактическое значение, вес, дата обновления факта
CREATE TABLE key_result (
    id              VARCHAR(32)   PRIMARY KEY,
    goal_id         VARCHAR(32)   NOT NULL REFERENCES goal (id) ON DELETE CASCADE,
    title           VARCHAR(500)  NOT NULL,
    metric_unit     VARCHAR(32),
    target_value    NUMERIC(18, 2),
    fact_value      NUMERIC(18, 2),
    weight          INTEGER       NOT NULL CHECK (weight BETWEEN 0 AND 100),
    fact_updated_at DATE          NOT NULL
);
CREATE INDEX ix_key_result_goal ON key_result (goal_id);
--rollback DROP TABLE key_result;
