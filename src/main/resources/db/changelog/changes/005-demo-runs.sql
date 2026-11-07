--liquibase formatted sql

-- Демо-история прогонов (еженедельно по понедельникам):
--   RUN-001, 12.10.2026 — успешный. Исторический результат: данные с тех пор поменялись,
--                         поэтому его проблемы и метрики лежат здесь как есть (так они лежали бы в БД).
--   RUN-002, 19.10.2026 — упавший (таймаут выгрузки). Результатов нет, API отдаёт 409 RUN_FAILED.
--   RUN-003, 26.10.2026 — его при старте выполняет сам движок по текущим данным (StartupRunInitializer)
--                         и сравнивает с RUN-001, пропуская упавший RUN-002.
-- Что изменилось в данных между RUN-001 и RUN-003:
--   исправили (resolved):  G-009 получила КР, у G-007 веса стали 50+50, G-023 привязали к G-015, факт KR-030 обновили 20.10;
--   появилось (new):       у G-022 удалили КР, у G-017 веса 60+50, G-020 отвязали от родителя,
--                          KR-025 (46 дн.) и KR-031 (55 дн.) перешагнули порог 45 дней;
--   сохраняется (persisting): остальные 8 проблем.
-- Набор целей и их статусы между прогонами не менялись — поэтому метрики RUN-001 можно перепроверить
-- движком (тест SeedDataTest).

--changeset kkd:005-demo-runs context:demo
--comment: Откат удаляет все прогоны: прогоны, которые позже выполнило приложение, построены на демо-данных и ссылаются на RUN-001
INSERT INTO kkd_run (id, number, status, trigger_type, snapshot_date, compared_to_run_id, started_at, finished_at, error) VALUES
('RUN-001', 1, 'completed', 'schedule', '2026-10-12', NULL,      '2026-10-12 06:00:00+03', '2026-10-12 06:07:41+03', NULL),
('RUN-002', 2, 'failed',    'schedule', '2026-10-19', 'RUN-001', '2026-10-19 06:00:00+03', '2026-10-19 06:04:12+03',
 'Таймаут выгрузки данных из системы управления целями. Результаты не сохранены.');
--rollback DELETE FROM kkd_run;

--changeset kkd:005-demo-run-001-issues context:demo
INSERT INTO kkd_issue (run_id, fingerprint, rule_code, severity, object_type, object_id, goal_id, kr_id, unit_id, details, actual_value, expected_value) VALUES
('RUN-001', 'GOAL_NO_KR:G-009',          'GOAL_NO_KR',        'critical', 'goal',       'G-009',  'G-009', NULL,     'CIB-KEY', 'У активной цели нет ключевых результатов', '0', '>= 1'),
('RUN-001', 'GOAL_NO_KR:G-013',          'GOAL_NO_KR',        'critical', 'goal',       'G-013',  'G-013', NULL,     'CIB-KEY', 'У активной цели нет ключевых результатов', '0', '>= 1'),
('RUN-001', 'KR_WEIGHTS_SUM:G-004',      'KR_WEIGHTS_SUM',    'major',    'goal',       'G-004',  'G-004', NULL,     'HQ-CIB',  'Сумма весов КР = 99%, ожидается 100%', '99', '100'),
('RUN-001', 'KR_WEIGHTS_SUM:G-007',      'KR_WEIGHTS_SUM',    'major',    'goal',       'G-007',  'G-007', NULL,     'CIB-KEY', 'Сумма весов КР = 80%, ожидается 100%', '80', '100'),
('RUN-001', 'KR_WEIGHTS_SUM:G-008',      'KR_WEIGHTS_SUM',    'major',    'goal',       'G-008',  'G-008', NULL,     'CIB-KEY', 'Сумма весов КР = 90%, ожидается 100%', '90', '100'),
('RUN-001', 'CASCADE_NO_PARENT:G-011',   'CASCADE_NO_PARENT', 'major',    'goal',       'G-011',  'G-011', NULL,     'CIB-KEY', 'Родительская цель не указана', NULL, 'цель вышестоящего уровня'),
('RUN-001', 'CASCADE_NO_PARENT:G-016',   'CASCADE_NO_PARENT', 'major',    'goal',       'G-016',  'G-016', NULL,     'CIB-REG', 'Родительская цель не указана', NULL, 'цель вышестоящего уровня'),
('RUN-001', 'CASCADE_NO_PARENT:G-023',   'CASCADE_NO_PARENT', 'major',    'goal',       'G-023',  'G-023', NULL,     'CIB-REG', 'Родительская цель не указана', NULL, 'цель вышестоящего уровня'),
('RUN-001', 'KR_STALE_PROGRESS:KR-018',  'KR_STALE_PROGRESS', 'minor',    'key_result', 'KR-018', 'G-010', 'KR-018', 'CIB-KEY', 'Факт обновлялся 53 дн. назад (2026-08-20)', '53', '<= 45'),
('RUN-001', 'KR_STALE_PROGRESS:KR-030',  'KR_STALE_PROGRESS', 'minor',    'key_result', 'KR-030', 'G-018', 'KR-030', 'CIB-REG', 'Факт обновлялся 63 дн. назад (2026-08-10)', '63', '<= 45'),
('RUN-001', 'KR_STALE_PROGRESS:KR-034',  'KR_STALE_PROGRESS', 'minor',    'key_result', 'KR-034', 'G-021', 'KR-034', 'CIB-REG', 'Факт обновлялся 89 дн. назад (2026-07-15)', '89', '<= 45'),
('RUN-001', 'KR_STALE_PROGRESS:KR-039',  'KR_STALE_PROGRESS', 'minor',    'key_result', 'KR-039', 'G-026', 'KR-039', 'CIB-KEY', 'Факт обновлялся 72 дн. назад (2026-08-01)', '72', '<= 45');
--rollback DELETE FROM kkd_issue WHERE run_id = 'RUN-001';

--changeset kkd:005-demo-run-001-metrics context:demo
--comment: Метрики RUN-001 по каждому подразделению с учётом дочерних. CIB-TRADE без целей — доли null
INSERT INTO kkd_run_metric (run_id, unit_id, metric_key, value) VALUES
('RUN-001', 'B2B',       'clean_goals_share', 47.8),
('RUN-001', 'B2B',       'cascade_coverage',  86.4),
('RUN-001', 'B2B',       'issues_total',      12),
('RUN-001', 'HQ-CIB',    'clean_goals_share', 45.5),
('RUN-001', 'HQ-CIB',    'cascade_coverage',  86.4),
('RUN-001', 'HQ-CIB',    'issues_total',      12),
('RUN-001', 'CIB-KEY',   'clean_goals_share', 22.2),
('RUN-001', 'CIB-KEY',   'cascade_coverage',  88.9),
('RUN-001', 'CIB-KEY',   'issues_total',      7),
('RUN-001', 'CIB-REG',   'clean_goals_share', 60.0),
('RUN-001', 'CIB-REG',   'cascade_coverage',  80.0),
('RUN-001', 'CIB-REG',   'issues_total',      4),
('RUN-001', 'CIB-TRADE', 'clean_goals_share', NULL),
('RUN-001', 'CIB-TRADE', 'cascade_coverage',  NULL),
('RUN-001', 'CIB-TRADE', 'issues_total',      0);
--rollback DELETE FROM kkd_run_metric WHERE run_id = 'RUN-001';
