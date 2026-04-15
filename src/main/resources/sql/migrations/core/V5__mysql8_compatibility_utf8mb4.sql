--
-- MySQL 8.0 Compatibility: Convert core tenants table from utf8 to utf8mb4
--
ALTER TABLE `tenant_server_connections` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
