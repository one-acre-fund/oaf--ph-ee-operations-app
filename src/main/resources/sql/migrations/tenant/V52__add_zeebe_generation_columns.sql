-- Each ALTER is skipped when the column already exists, so a tenant where an earlier run failed
-- halfway (acc-qa oaf and kenya, 2026-10-22) can be repaired and rerun.
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transfers' AND COLUMN_NAME = 'ZEEBE_GENERATION'
        ),
        'SELECT 1',
        'ALTER TABLE transfers ADD COLUMN ZEEBE_GENERATION BIGINT(20) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction_requests' AND COLUMN_NAME = 'ZEEBE_GENERATION'
        ),
        'SELECT 1',
        'ALTER TABLE transaction_requests ADD COLUMN ZEEBE_GENERATION BIGINT(20) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'batches' AND COLUMN_NAME = 'ZEEBE_GENERATION'
        ),
        'SELECT 1',
        'ALTER TABLE batches ADD COLUMN ZEEBE_GENERATION BIGINT(20) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'ZEEBE_GENERATION'
        ),
        'SELECT 1',
        'ALTER TABLE tasks ADD COLUMN ZEEBE_GENERATION BIGINT(20) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'variables' AND COLUMN_NAME = 'ZEEBE_GENERATION'
        ),
        'SELECT 1',
        'ALTER TABLE variables ADD COLUMN ZEEBE_GENERATION BIGINT(20) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'businesskeys' AND COLUMN_NAME = 'ZEEBE_GENERATION'
        ),
        'SELECT 1',
        'ALTER TABLE businesskeys ADD COLUMN ZEEBE_GENERATION BIGINT(20) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Index to be added manually because of the time it takes
--CREATE INDEX idx_transfers_key_gen ON transfers (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION);
--CREATE INDEX idx_transaction_requests_key_gen ON transaction_requests (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION);
--CREATE INDEX idx_batches_key_gen ON batches (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION);
--CREATE INDEX idx_tasks_key_gen ON tasks (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION);
--CREATE INDEX idx_variables_key_gen ON variables (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION);
--CREATE INDEX idx_businesskeys_key_gen ON businesskeys (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION);
