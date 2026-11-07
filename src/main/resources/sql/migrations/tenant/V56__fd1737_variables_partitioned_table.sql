-- FD-1737: replacement for the variables table, monthly RANGE partitions on TIMESTAMP (ms epoch, UTC)
-- so a month is archived and dropped with one metadata operation, compressed rows, the composite index
-- used by the importer dedup read and the detail view, without the unused transaction_requests_id and
-- audit columns. The partition column must be part of the primary key; ID stays AUTO_INCREMENT.
-- Only the empty table is created here. Filling it and the RENAME swap are an operational step
-- (copy the retained window while the importer runs, pause it, copy the tail, set AUTO_INCREMENT to
-- MAX(ID) + 1 of the old table, RENAME, resume); see the FD-1737 plan in helm-paymenthub.
-- Partitions cover 2025-11 to 2027-03; the monthly retention job reorganizes pmax to add the next one.
CREATE TABLE IF NOT EXISTS `variables_new` (
   `ID` bigint(20) NOT NULL AUTO_INCREMENT,
   `WORKFLOW_INSTANCE_KEY` bigint(20) NOT NULL,
   `WORKFLOW_KEY` bigint(20) DEFAULT NULL,
   `ZEEBE_GENERATION` bigint(20) NOT NULL DEFAULT 0,
   `TIMESTAMP` bigint(20) NOT NULL,
   `NAME` varchar(255) NOT NULL,
   `VALUE` longtext,
   PRIMARY KEY (`ID`, `TIMESTAMP`),
   KEY `idx_variables_key` (`WORKFLOW_INSTANCE_KEY`, `ZEEBE_GENERATION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8
PARTITION BY RANGE (`TIMESTAMP`) (
   PARTITION `p2025_11` VALUES LESS THAN (1764547200000),
   PARTITION `p2025_12` VALUES LESS THAN (1767225600000),
   PARTITION `p2026_01` VALUES LESS THAN (1769904000000),
   PARTITION `p2026_02` VALUES LESS THAN (1772323200000),
   PARTITION `p2026_03` VALUES LESS THAN (1775001600000),
   PARTITION `p2026_04` VALUES LESS THAN (1777593600000),
   PARTITION `p2026_05` VALUES LESS THAN (1780272000000),
   PARTITION `p2026_06` VALUES LESS THAN (1782864000000),
   PARTITION `p2026_07` VALUES LESS THAN (1785542400000),
   PARTITION `p2026_08` VALUES LESS THAN (1788220800000),
   PARTITION `p2026_09` VALUES LESS THAN (1790812800000),
   PARTITION `p2026_10` VALUES LESS THAN (1793491200000),
   PARTITION `p2026_11` VALUES LESS THAN (1796083200000),
   PARTITION `p2026_12` VALUES LESS THAN (1798761600000),
   PARTITION `p2027_01` VALUES LESS THAN (1801440000000),
   PARTITION `p2027_02` VALUES LESS THAN (1803859200000),
   PARTITION `p2027_03` VALUES LESS THAN (1806537600000),
   PARTITION `pmax` VALUES LESS THAN MAXVALUE
);
