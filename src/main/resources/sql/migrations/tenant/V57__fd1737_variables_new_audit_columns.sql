-- FD-1737: V56 left the JPA audit columns out of variables_new, but the Variable entity inherits them from
-- AbstractPersistableCustom, so every operations-app query on the table would fail with "Unknown column"
-- once variables_new is renamed to variables. Adds the same four columns variables has (V45), while the
-- table is still empty. Skipped where variables_new is missing or already has them.
SET @sql = (
    SELECT IF(
        NOT EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'variables_new'
        ) OR EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'variables_new'
              AND COLUMN_NAME = 'created_date'
        ),
        'SELECT 1',
        'ALTER TABLE `variables_new` ADD COLUMN `created_date` timestamp NULL DEFAULT NULL, ADD COLUMN `last_modified_date` timestamp NULL DEFAULT NULL, ADD COLUMN `created_by` varchar(255) DEFAULT NULL, ADD COLUMN `last_modified_by` varchar(255) DEFAULT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
