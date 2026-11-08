package org.apache.fineract.retention;

import org.apache.fineract.export.TenantContext;
import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

/**
 * FD-1737: monthly housekeeping of the partitioned variables table, for every tenant. Adds the empty months
 * ahead, then archives to S3 and drops every month older than the retention window, oldest first. A tenant whose
 * table is not partitioned yet is skipped. A failed archive keeps its partition and stops the dropping for that
 * tenant until the next run; the error log line is the alert.
 */
@Component
@ConditionalOnProperty(value = "variables.retention.enabled", havingValue = "true")
public class VariableRetentionJob {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TenantContext tenantContext;
    private final VariableTableOps table;
    private final VariableArchive archive;
    private final VariableRetentionProperties properties;
    private final Clock clock;

    public VariableRetentionJob(TenantContext tenantContext, VariableTableOps table, VariableArchive archive,
                                VariableRetentionProperties properties, @Qualifier("exportClock") Clock clock) {
        this.tenantContext = tenantContext;
        this.table = table;
        this.archive = archive;
        this.properties = properties;
        this.clock = clock;
    }

    /** Default: 03:00 UTC on the 2nd of every month, after the month has fully ended in every time zone. */
    @Scheduled(cron = "${variables.retention.cron:0 0 3 2 * *}", zone = "UTC")
    public void run() {
        for (TenantServerConnection tenant : tenantContext.allTenants()) {
            try {
                tenantContext.runAs(tenant, () -> retain(tenant.getSchemaName()));
            } catch (RuntimeException e) {
                logger.error("Variables retention failed for tenant {}", tenant.getSchemaName(), e);
            }
        }
    }

    void retain(String tenant) {
        if (!table.tryLock(tenant)) {
            logger.info("Tenant {}: variables retention already running elsewhere, skipped", tenant);
            return;
        }
        try {
            List<String> partitions = table.partitionNames();
            if (partitions.isEmpty()) {
                logger.info("Tenant {}: variables is not partitioned, nothing to retain", tenant);
                return;
            }
            if (!partitions.contains(VariablePartitions.PMAX)) {
                logger.error("Tenant {}: variables has no {} partition, not touching it", tenant, VariablePartitions.PMAX);
                return;
            }
            YearMonth current = YearMonth.now(clock.withZone(ZoneOffset.UTC));
            addMonthsAhead(tenant, partitions, current);
            archiveAndDrop(tenant, partitions, current);
        } finally {
            table.unlock(tenant);
        }
    }

    private void addMonthsAhead(String tenant, List<String> partitions, YearMonth current) {
        long strays = table.rowCount(VariablePartitions.PMAX);
        if (strays > 0) {
            logger.error("Tenant {}: {} rows in {}, months were not added ahead in time; reorganizing moves them",
                    tenant, strays, VariablePartitions.PMAX);
        }
        for (YearMonth month : VariablePartitions.toAdd(partitions, current, properties.getAheadMonths())) {
            if (properties.isDryRun()) {
                logger.info("Tenant {}: dry run, would add partition {}", tenant, VariablePartitions.name(month));
                continue;
            }
            table.addMonth(month);
            logger.info("Tenant {}: added partition {}", tenant, VariablePartitions.name(month));
        }
    }

    private void archiveAndDrop(String tenant, List<String> partitions, YearMonth current) {
        for (String partition : VariablePartitions.toDrop(partitions, current, properties.getMonths())) {
            long rows = table.rowCount(partition);
            if (properties.isDryRun()) {
                logger.info("Tenant {}: dry run, would archive {} rows of {} to {} and drop it", tenant, rows,
                        partition, archive.keyFor(tenant, partition));
                continue;
            }
            if (rows > 0) {
                try {
                    long written = archive.archive(tenant, partition);
                    logger.info("Tenant {}: archived {} rows of {} to {}", tenant, written, partition,
                            archive.keyFor(tenant, partition));
                } catch (RuntimeException e) {
                    logger.error("Tenant {}: archiving {} failed, partition kept; retrying next run", tenant,
                            partition, e);
                    return;
                }
            }
            table.dropPartition(partition);
            logger.info("Tenant {}: dropped partition {} ({} rows)", tenant, partition, rows);
        }
    }
}
