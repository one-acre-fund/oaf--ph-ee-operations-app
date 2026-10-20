ALTER TABLE `export_jobs`
    ADD COLUMN `heartbeat_at` datetime DEFAULT NULL,
    ADD COLUMN `attempts` int NOT NULL DEFAULT 0,
    ADD COLUMN `releases` int NOT NULL DEFAULT 0;
