--liquibase formatted sql

--changeset kkd:003-rule-catalog
--comment: Четыре правила, которые реализует движок (engine/rules). Код и важность совпадают с классами правил
INSERT INTO kkd_rule (code, title, severity, object_type, description, recommendation) VALUES
('GOAL_NO_KR', 'Цель без ключевых результатов', 'critical', 'goal',
 'У активной цели нет ни одного ключевого результата.',
 'Добавить 1–5 измеримых ключевых результатов.'),
('KR_WEIGHTS_SUM', 'Сумма весов КР не равна 100%', 'major', 'goal',
 'Сумма весов ключевых результатов цели отличается от 100%.',
 'Перераспределить веса КР так, чтобы сумма была 100%.'),
('CASCADE_NO_PARENT', 'Цель не связана с вышестоящей', 'major', 'goal',
 'Цель департамента, подразделения или сотрудника не привязана к цели вышестоящего уровня.',
 'Указать родительскую цель вышестоящего уровня.'),
('KR_STALE_PROGRESS', 'Прогресс КР давно не обновлялся', 'minor', 'key_result',
 'Фактическое значение КР не обновлялось более 45 дней.',
 'Обновить фактическое значение КР.');
--rollback DELETE FROM kkd_rule WHERE code IN ('GOAL_NO_KR', 'KR_WEIGHTS_SUM', 'CASCADE_NO_PARENT', 'KR_STALE_PROGRESS');
