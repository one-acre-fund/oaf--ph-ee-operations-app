ALTER TABLE m_appuser
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_appuser
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_appuser
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_appuser
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_appuser_previous_password
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_appuser_previous_password
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_appuser_previous_password
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_appuser_previous_password
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_audit_source
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_audit_source
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_audit_source
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_audit_source
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE batches
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE batches
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE batches
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE batches
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_beneficiary
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_beneficiary
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_beneficiary
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_beneficiary
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE businesskeys
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE businesskeys
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE businesskeys
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE businesskeys
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_code
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_code
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_code
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_code
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_code_value
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_code_value
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_code_value
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_code_value
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_currency_rates
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_currency_rates
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_currency_rates
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_currency_rates
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_currency_rates_lock
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_currency_rates_lock
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_currency_rates_lock
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_currency_rates_lock
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_document
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_document
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_document
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_document
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE errorcode
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE errorcode
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE errorcode
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE errorcode
ADD COLUMN last_modified_by VARCHAR(255);


ALTER TABLE m_group
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_group
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_group
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_group
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_group_level
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_group_level
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_group_level
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_group_level
ADD COLUMN last_modified_by VARCHAR(255);


ALTER TABLE m_group_roles
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_group_roles
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_group_roles
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_group_roles
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_image
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_image
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_image
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_image
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_office
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_office
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_office
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_office
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_password_validation_policy
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_password_validation_policy
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_password_validation_policy
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_password_validation_policy
ADD COLUMN last_modified_by VARCHAR(255);


ALTER TABLE m_permission
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_permission
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_permission
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_permission
ADD COLUMN last_modified_by VARCHAR(255);


ALTER TABLE m_role
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_role
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_role
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_role
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE m_staff
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE m_staff
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE m_staff
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE m_staff
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE tasks
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE tasks
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE tasks
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE tasks
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE transaction_requests
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE transaction_requests
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE transaction_requests
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE transaction_requests
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE transfers
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE transfers
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE transfers
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE transfers
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE variables
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE variables
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE variables
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE variables
ADD COLUMN last_modified_by VARCHAR(255);


ALTER TABLE tenant_server_connections
ADD COLUMN created_date TIMESTAMP NULL;
ALTER TABLE tenant_server_connections
ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE tenant_server_connections
ADD COLUMN created_by VARCHAR(255);
ALTER TABLE tenant_server_connections
ADD COLUMN last_modified_by VARCHAR(255);