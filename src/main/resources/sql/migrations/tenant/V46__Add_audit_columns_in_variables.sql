ALTER TABLE variables
    ADD COLUMN IF NOT EXISTS created_date TIMESTAMP NULL;
ALTER TABLE variables
    ADD COLUMN IF NOT EXISTS last_modified_date TIMESTAMP NULL;
ALTER TABLE variables
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE variables
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255);