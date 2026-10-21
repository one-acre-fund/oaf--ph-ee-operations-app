ALTER TABLE `export_jobs`
    ADD COLUMN `heartbeat_at` datetime DEFAULT NULL,
    ADD COLUMN `attempts` int NOT NULL DEFAULT 0,
    ADD COLUMN `releases` int NOT NULL DEFAULT 0;

-- jobs left RUNNING by the previous version have no heartbeat; without one, recovery never takes them back
UPDATE `export_jobs` SET `heartbeat_at` = `started_at`, `attempts` = 1 WHERE `status` = 'RUNNING';
