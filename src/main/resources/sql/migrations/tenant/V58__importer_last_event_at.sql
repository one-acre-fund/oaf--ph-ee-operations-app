-- Flyway Migration Script
-- Description: ph-ee-importer-rdbms generation tracking. LAST_EVENT_AT holds the Zeebe timestamp of the
-- newest record the importer applied to a transfer, transaction request or batch row. The importer uses
-- it to tell a generation that is still running from one whose process Zeebe lost (broker reset, the
-- instance never reached ELEMENT_COMPLETED): when a record for the same workflow instance key arrives
-- after the row has been idle longer than importer.generation.max-idle-minutes, the old row is closed and
-- the record opens a new ZEEBE_GENERATION instead of overwriting the old payment.
-- Must be applied before the importer version that maps LAST_EVENT_AT starts.
--
-- Nullable, no default and appended at the end of the table, so on MySQL 8.0.12+ every ALTER below is an
-- INSTANT metadata change: no table rebuild and no row lock on the large transfers table. Existing rows
-- read NULL and the importer falls back to STARTED_AT for them. DATETIME(3) keeps the milliseconds.
-- No index: the importer always reads it from rows already found by WORKFLOW_INSTANCE_KEY.
-- Each ALTER skips a column that already exists, so the script can be rerun after a partial failure.

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transfers' AND COLUMN_NAME = 'LAST_EVENT_AT'), 'SELECT 1', 'ALTER TABLE transfers ADD COLUMN LAST_EVENT_AT DATETIME(3) NULL'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction_requests' AND COLUMN_NAME = 'LAST_EVENT_AT'), 'SELECT 1', 'ALTER TABLE transaction_requests ADD COLUMN LAST_EVENT_AT DATETIME(3) NULL'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'batches' AND COLUMN_NAME = 'LAST_EVENT_AT'), 'SELECT 1', 'ALTER TABLE batches ADD COLUMN LAST_EVENT_AT DATETIME(3) NULL'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
