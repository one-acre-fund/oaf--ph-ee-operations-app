-- Flyway Migration Script
-- Description: Add db indexes to transaction request, transfer, tasks and variables tables
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND INDEX_NAME = 'idx_transaction_requests_key'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND COLUMN_NAME IN ('WORKFLOW_INSTANCE_KEY')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_transaction_requests_key ON transaction_requests (WORKFLOW_INSTANCE_KEY)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND INDEX_NAME = 'idx_tr_cil_asd'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND COLUMN_NAME IN ('CLIENTCORRELATIONID', 'STARTED_AT')
        ) <> 2,
        'SELECT 1',
        'CREATE INDEX idx_tr_cil_asd ON transaction_requests (CLIENTCORRELATIONID, STARTED_AT)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND INDEX_NAME = 'idx_dir_startedat'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND COLUMN_NAME IN ('DIRECTION', 'STARTED_AT')
        ) <> 2,
        'SELECT 1',
        'CREATE INDEX idx_dir_startedat ON transaction_requests (DIRECTION, STARTED_AT)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND INDEX_NAME = 'idx_tr_ppid'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND COLUMN_NAME IN ('PAYEE_PARTY_ID')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_tr_ppid ON transaction_requests (PAYEE_PARTY_ID)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND INDEX_NAME = 'idx_tr_EXTERNAL_ID'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND COLUMN_NAME IN ('EXTERNAL_ID')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_tr_EXTERNAL_ID ON transaction_requests (EXTERNAL_ID)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND INDEX_NAME = 'idx_tr_TRANSACTION_ID'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND COLUMN_NAME IN ('TRANSACTION_ID')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_tr_TRANSACTION_ID ON transaction_requests (TRANSACTION_ID)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND INDEX_NAME = 'idx_currency_completed_at'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transaction_requests'
              AND COLUMN_NAME IN ('CURRENCY', 'COMPLETED_AT')
        ) <> 2,
        'SELECT 1',
        'CREATE INDEX idx_currency_completed_at ON transaction_requests (CURRENCY, COMPLETED_AT)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND INDEX_NAME = 'idx_transfers_key'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND COLUMN_NAME IN ('WORKFLOW_INSTANCE_KEY')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_transfers_key ON transfers (WORKFLOW_INSTANCE_KEY)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND INDEX_NAME = 'idx_transfers_tid_sd'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND COLUMN_NAME IN ('TRANSACTION_ID', 'STARTED_AT')
        ) <> 2,
        'SELECT 1',
        'CREATE INDEX idx_transfers_tid_sd ON transfers (TRANSACTION_ID, STARTED_AT)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND INDEX_NAME = 'idx_transfers_sat'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND COLUMN_NAME IN ('STARTED_AT')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_transfers_sat ON transfers (STARTED_AT)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND INDEX_NAME = 'idx_tra_ppid'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND COLUMN_NAME IN ('PAYEE_PARTY_ID')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_tra_ppid ON transfers (PAYEE_PARTY_ID)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND INDEX_NAME = 'idx_currency_direction_started'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND COLUMN_NAME IN ('CURRENCY', 'DIRECTION', 'STARTED_AT')
        ) <> 3,
        'SELECT 1',
        'CREATE INDEX idx_currency_direction_started ON transfers (CURRENCY, DIRECTION, STARTED_AT)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND INDEX_NAME = 'idx_transfers_cliid'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND COLUMN_NAME IN ('CLIENTCORRELATIONID')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_transfers_cliid ON transfers (CLIENTCORRELATIONID)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND INDEX_NAME = 'idx_payer_direction_started'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transfers'
              AND COLUMN_NAME IN ('PAYER_PARTY_ID', 'DIRECTION', 'STARTED_AT')
        ) <> 3,
        'SELECT 1',
        'CREATE INDEX idx_payer_direction_started ON transfers (PAYER_PARTY_ID, DIRECTION, STARTED_AT)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tasks'
              AND INDEX_NAME = 'idx_tasks_key'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'tasks'
              AND COLUMN_NAME IN ('WORKFLOW_INSTANCE_KEY')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_tasks_key ON tasks (WORKFLOW_INSTANCE_KEY)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'variables'
              AND INDEX_NAME = 'idx_variables_key'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'variables'
              AND COLUMN_NAME IN ('WORKFLOW_INSTANCE_KEY')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX idx_variables_key ON variables (WORKFLOW_INSTANCE_KEY)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'variables'
              AND INDEX_NAME = 'variables_transaction_requests_id_foreign'
        ) OR (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'variables'
              AND COLUMN_NAME IN ('transaction_requests_id')
        ) <> 1,
        'SELECT 1',
        'CREATE INDEX variables_transaction_requests_id_foreign ON variables (transaction_requests_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
