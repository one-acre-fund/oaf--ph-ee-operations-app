
ALTER TABLE tenant_server_connections
    ADD COLUMN last_modified_date TIMESTAMP NULL;
ALTER TABLE tenant_server_connections
    ADD COLUMN created_by VARCHAR(255);
ALTER TABLE tenant_server_connections
    ADD COLUMN last_modified_by VARCHAR(255);