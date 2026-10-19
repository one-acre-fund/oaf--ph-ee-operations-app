-- FD-1929: asynchronous CSV exports
CREATE TABLE IF NOT EXISTS `export_jobs` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
   `job_id` varchar(36) NOT NULL,
   `tenant` varchar(100) NOT NULL,
   `export_type` varchar(32) NOT NULL,
   `status` varchar(16) NOT NULL,
   `requested_by` varchar(100) NOT NULL,
   `filters` text DEFAULT NULL,
   `data_scope` text DEFAULT NULL,
   `row_count` bigint(20) DEFAULT NULL,
   `file_size_bytes` bigint(20) DEFAULT NULL,
   `storage_key` varchar(512) DEFAULT NULL,
   `error_code` varchar(64) DEFAULT NULL,
   `error_message` varchar(1000) DEFAULT NULL,
   `started_at` datetime DEFAULT NULL,
   `completed_at` datetime DEFAULT NULL,
   `expires_at` datetime DEFAULT NULL,
   `created_date` datetime DEFAULT NULL,
   `last_modified_date` datetime DEFAULT NULL,
   `created_by` varchar(100) DEFAULT NULL,
   `last_modified_by` varchar(100) DEFAULT NULL,
   PRIMARY KEY (`id`),
   UNIQUE KEY `uq_export_jobs_job_id` (`job_id`),
   KEY `idx_export_jobs_user_created` (`requested_by`, `created_date`),
   KEY `idx_export_jobs_status_expires` (`status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Keyset pagination on transaction_requests seeks on (STARTED_AT, ID). transfers already has
-- idx_transfers_sat (V51); InnoDB secondary indexes include the primary key, so that covers (STARTED_AT, ID).
-- Index to be added manually because of the time it takes on large tables:
--CREATE INDEX idx_transaction_requests_started_at ON transaction_requests (STARTED_AT);
