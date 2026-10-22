-- Flyway Migration Script
-- Description: FD-1931, importer horizontal scaling. Unique keys that make importer writes
-- idempotent under several replicas and Kafka redelivery, optimistic lock columns, and the
-- call-activity routing table. Must be applied before ph-ee-importer-rdbms 1.2.0 starts.
--
-- Duplicates created by the single-instance importer are removed first, keeping the row with the
-- lowest ID. Transfer identity is the transaction id, or '' for an ordinary transfer that has none
-- yet (two such rows for one instance are duplicates). A batch CSV row without a request id keeps
-- NULL, which never compares equal, so distinct CSV rows survive. TRANSFER_KEY below uses the
-- same expression. Requires MySQL 5.7.8 or later (virtual generated column in a unique index).

-- Duplicate IDs are collected with plain SELECTs into temporary tables first and then deleted by
-- primary key. A DELETE with a self join locks every row it scans, and on a tenant with millions of
-- task rows that exceeded the InnoDB lock table (seen in acc-qa, rwanda and burundi, 2026-10-22).

CREATE TEMPORARY TABLE dup_transfers AS
SELECT a.ID FROM transfers a JOIN transfers b
  ON a.WORKFLOW_INSTANCE_KEY = b.WORKFLOW_INSTANCE_KEY
 AND a.ZEEBE_GENERATION <=> b.ZEEBE_GENERATION
 AND IFNULL(a.TRANSACTION_ID, IF(a.BATCH_ID IS NULL, '', NULL))
     = IFNULL(b.TRANSACTION_ID, IF(b.BATCH_ID IS NULL, '', NULL))
 AND a.ID > b.ID;
DELETE a FROM transfers a JOIN dup_transfers d ON d.ID = a.ID;
DROP TEMPORARY TABLE dup_transfers;

CREATE TEMPORARY TABLE dup_transaction_requests AS
SELECT a.ID FROM transaction_requests a JOIN transaction_requests b
  ON a.WORKFLOW_INSTANCE_KEY = b.WORKFLOW_INSTANCE_KEY
 AND a.ZEEBE_GENERATION <=> b.ZEEBE_GENERATION
 AND a.ID > b.ID;
DELETE a FROM transaction_requests a JOIN dup_transaction_requests d ON d.ID = a.ID;
DROP TEMPORARY TABLE dup_transaction_requests;

CREATE TEMPORARY TABLE dup_batches AS
SELECT a.ID FROM batches a JOIN batches b
  ON a.WORKFLOW_INSTANCE_KEY = b.WORKFLOW_INSTANCE_KEY
 AND a.ZEEBE_GENERATION <=> b.ZEEBE_GENERATION
 AND a.ID > b.ID;
DELETE a FROM batches a JOIN dup_batches d ON d.ID = a.ID;
DROP TEMPORARY TABLE dup_batches;

CREATE TEMPORARY TABLE dup_variables AS
SELECT a.ID FROM variables a JOIN variables b
  ON a.WORKFLOW_INSTANCE_KEY = b.WORKFLOW_INSTANCE_KEY
 AND a.ZEEBE_GENERATION <=> b.ZEEBE_GENERATION
 AND a.NAME = b.NAME AND a.TIMESTAMP = b.TIMESTAMP
 AND a.ID > b.ID;
DELETE a FROM variables a JOIN dup_variables d ON d.ID = a.ID;
DROP TEMPORARY TABLE dup_variables;

CREATE TEMPORARY TABLE dup_tasks AS
SELECT a.ID FROM tasks a JOIN tasks b
  ON a.WORKFLOW_INSTANCE_KEY = b.WORKFLOW_INSTANCE_KEY
 AND a.ZEEBE_GENERATION <=> b.ZEEBE_GENERATION
 AND a.ELEMENT_ID = b.ELEMENT_ID AND a.INTENT = b.INTENT
 AND a.ID > b.ID;
DELETE a FROM tasks a JOIN dup_tasks d ON d.ID = a.ID;
DROP TEMPORARY TABLE dup_tasks;

-- Optimistic lock for the rows the importer updates in place. Existing rows read as version 0.
ALTER TABLE transfers ADD COLUMN VERSION BIGINT NOT NULL DEFAULT 0;
ALTER TABLE transaction_requests ADD COLUMN VERSION BIGINT NOT NULL DEFAULT 0;
ALTER TABLE batches ADD COLUMN VERSION BIGINT NOT NULL DEFAULT 0;

-- MySQL lets any number of NULLs through a unique index and an ordinary transfer has
-- TRANSACTION_ID NULL until its variable arrives, so the key goes on a generated column instead.
-- VIRTUAL: no table rebuild, only the index is built.
ALTER TABLE transfers ADD COLUMN TRANSFER_KEY VARCHAR(255)
  GENERATED ALWAYS AS (IFNULL(TRANSACTION_ID, IF(BATCH_ID IS NULL, '', NULL))) VIRTUAL;

ALTER TABLE transfers ADD UNIQUE KEY uk_transfers_instance_generation_key
  (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION, TRANSFER_KEY);
ALTER TABLE transaction_requests ADD UNIQUE KEY uk_transaction_requests_instance_generation
  (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION);
ALTER TABLE batches ADD UNIQUE KEY uk_batches_instance_generation
  (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION);
ALTER TABLE variables ADD UNIQUE KEY uk_variables_instance_generation_name_timestamp
  (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION, NAME, TIMESTAMP);
ALTER TABLE tasks ADD UNIQUE KEY uk_tasks_instance_generation_element_intent
  (WORKFLOW_INSTANCE_KEY, ZEEBE_GENERATION, ELEMENT_ID, INTENT);

-- Child process instance (call activity) to parent transfer, kept by the importer so child
-- variables find their parent after a rebalance or a pod loss.
CREATE TABLE IF NOT EXISTS call_activity_links (
  ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  CHILD_INSTANCE_KEY BIGINT NOT NULL,
  PARENT_INSTANCE_KEY BIGINT NOT NULL,
  UNIQUE KEY uk_call_activity_links_child (CHILD_INSTANCE_KEY)
);
