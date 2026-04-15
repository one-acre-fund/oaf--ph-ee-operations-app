--
-- MySQL 8.0 Compatibility Migration
-- 1. Convert all tables from utf8/utf8mb3 to utf8mb4 for full Unicode support
-- 2. Escape 'grouping' column name with backticks (reserved keyword in MySQL 8.0)
--

-- ============================================================
-- ISSUE 1: Convert charset from utf8 (utf8mb3) to utf8mb4
-- ============================================================

-- Core tables from V2
ALTER TABLE `m_code` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_code_value` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_document` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_office` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_permission` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_role` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_role_permission` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_appuser` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_appuser_role` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_staff` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_group_level` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_group` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- V10: m_image
ALTER TABLE `m_image` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- V11: m_group_roles
ALTER TABLE `m_group_roles` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- V22: m_password_validation_policy
ALTER TABLE `m_password_validation_policy` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- V24: oauth_client_details
ALTER TABLE `oauth_client_details` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- Remaining tables that may have inherited the database default charset
ALTER TABLE `m_appuser_previous_password` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_audit_source` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_beneficiary` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_currency_rates` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `m_currency_rates_lock` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `transfers` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `transaction_requests` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `tasks` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `variables` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `batches` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `businesskeys` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `errorcode` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
