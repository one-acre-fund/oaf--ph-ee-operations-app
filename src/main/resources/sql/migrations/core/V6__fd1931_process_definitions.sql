-- Flyway Migration Script
-- Description: FD-1931, importer horizontal scaling. Zeebe process definition key to
-- bpmnProcessId-tenant, learned by ph-ee-importer-rdbms from DEPLOYMENT records. Shared across
-- importer replicas, so a replica that never consumed the deployment can still route VARIABLE
-- and JOB records that carry no bpmnProcessId. A partition stuck on "is not registered yet" means
-- the deployment record was never seen; inserting the mapping here releases it.
CREATE TABLE IF NOT EXISTS process_definitions (
  ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  PROCESS_DEFINITION_KEY BIGINT NOT NULL,
  BPMN_PROCESS_ID VARCHAR(255) NOT NULL,
  UNIQUE KEY uk_process_definitions_key (PROCESS_DEFINITION_KEY)
);
