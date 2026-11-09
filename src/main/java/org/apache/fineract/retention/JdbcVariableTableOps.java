package org.apache.fineract.retention;

import org.apache.fineract.core.service.DataSourcePerTenantService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/** Runs on the tenant set on the thread (see TenantContext), like the Flyway upgrade does. */
@Component
class JdbcVariableTableOps implements VariableTableOps {

    private final DataSourcePerTenantService dataSources;

    JdbcVariableTableOps(DataSourcePerTenantService dataSources) {
        this.dataSources = dataSources;
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSources.retrieveDataSource());
    }

    @Override
    public boolean tryLock(String tenant) {
        Integer got = jdbc().queryForObject("SELECT GET_LOCK(?, 0)", Integer.class, lockName(tenant));
        return got != null && got == 1;
    }

    @Override
    public void unlock(String tenant) {
        jdbc().queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, lockName(tenant));
    }

    private static String lockName(String tenant) {
        return "variables_retention_" + tenant;
    }

    @Override
    public List<String> partitionNames() {
        return jdbc().queryForList("SELECT PARTITION_NAME FROM INFORMATION_SCHEMA.PARTITIONS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'variables' AND PARTITION_NAME IS NOT NULL"
                + " ORDER BY PARTITION_ORDINAL_POSITION", String.class);
    }

    @Override
    public long rowCount(String partition) {
        String sql = "SELECT COUNT(*) FROM variables PARTITION (" + VariablePartitions.checked(partition) + ")";
        Long count = jdbc().queryForObject(sql, Long.class); // NOSONAR partition name checked against pmax|pYYYY_MM
        return count == null ? 0 : count;
    }

    @Override
    public void addMonth(YearMonth month) {
        String sql = "ALTER TABLE variables REORGANIZE PARTITION " + VariablePartitions.PMAX + " INTO ("
                + "PARTITION " + VariablePartitions.name(month) + " VALUES LESS THAN ("
                + VariablePartitions.upperBoundMillis(month) + "), "
                + "PARTITION " + VariablePartitions.PMAX + " VALUES LESS THAN MAXVALUE)";
        jdbc().execute(sql); // NOSONAR built from a YearMonth and constants only
    }

    @Override
    public void dropPartition(String partition) {
        String sql = "ALTER TABLE variables DROP PARTITION " + VariablePartitions.checked(partition);
        jdbc().execute(sql); // NOSONAR partition name checked against pmax|pYYYY_MM
    }
}
