-- Flyway Migration Script
-- Description: Insert initial roles, update Super user name, and assign permissions

-- Step 1: Update 'Super user' to 'super_user' if it exists
UPDATE m_role SET name = 'super_user' WHERE name = 'Super user';

-- Step 2: Insert roles if they do not already exist
INSERT INTO m_role (name, description, is_disabled, created_date, last_modified_date, created_by)
SELECT name, description, is_disabled, created_date, last_modified_date, created_by
FROM (SELECT 'super_user'                                      AS name,
             'This role provides all application permissions.' AS description,
             0                                                 AS is_disabled,
             NULL                                              AS created_date,
             NULL                                              AS last_modified_date,
             NULL                                              AS created_by
      UNION ALL
      SELECT 'tech_admin',
             'Role to be assigned to the Tech Admin',
             0,
             CAST(DATE (NOW()) AS DATETIME),
             CAST(DATE (NOW()) AS DATETIME),
             'migration'
      UNION ALL
      SELECT 'global_cdm',
             'Role to be assigned to global CDM teams',
             0,
             CAST(DATE (NOW()) AS DATETIME),
             CAST(DATE (NOW()) AS DATETIME),
             'migration'
      UNION ALL
      SELECT 'country_cdm',
             'Role to be assigned to the back office biz ops team',
             0,
             CAST(DATE (NOW()) AS DATETIME),
             CAST(DATE (NOW()) AS DATETIME),
             'migration'
      UNION ALL
      SELECT 'ce_agent',
             'Role to be assigned to country customer experience team',
             0,
             CAST(DATE (NOW()) AS DATETIME),
             CAST(DATE (NOW()) AS DATETIME),
             'migration'
      UNION ALL
      SELECT 'audit_user',
             'Role to be assigned to country audit user.',
             0,
             CAST(DATE (NOW()) AS DATETIME),
             CAST(DATE (NOW()) AS DATETIME),
             'migration') AS new_roles
WHERE NOT EXISTS (SELECT 1
                  FROM m_role
                  WHERE name = new_roles.name);

-- Step 3: Insert role-permission mappings, ignoring duplicates
INSERT IGNORE INTO m_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'audit_user'  AND p.code = 'EXPORT_TRANSACTION_REQUEST' UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'audit_user'  AND p.code = 'EXPORT_TRANSFER'            UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'audit_user'  AND p.code = 'READ_AUDIT'                 UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'audit_user'  AND p.code = 'READ_TRANSACTION_REQUEST'   UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'audit_user'  AND p.code = 'READ_TRANSFER'              UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'ce_agent'    AND p.code = 'READ_TRANSACTION_REQUEST'   UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'ce_agent'    AND p.code = 'READ_TRANSFER'              UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'country_cdm' AND p.code = 'EXPORT_TRANSACTION_REQUEST' UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'country_cdm' AND p.code = 'EXPORT_TRANSFER'            UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'country_cdm' AND p.code = 'READ_TRANSACTION_REQUEST'   UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'country_cdm' AND p.code = 'READ_TRANSFER'              UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'global_cdm'  AND p.code = 'READ_TRANSACTION_REQUEST'   UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'global_cdm'  AND p.code = 'READ_TRANSFER'              UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'super_user'  AND p.code = 'ALL_FUNCTIONS'              UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'tech_admin'  AND p.code = 'READ_TRANSACTION_REQUEST'   UNION ALL
SELECT r.id, p.id FROM m_role r JOIN m_permission p ON r.name = 'tech_admin'  AND p.code = 'READ_TRANSFER';