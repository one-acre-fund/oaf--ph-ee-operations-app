package org.apache.fineract.retention;

import org.apache.fineract.core.service.DataSourcePerTenantService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Runs on the tenant set on the thread (see TenantContext), like the Flyway upgrade does. */
@Component
class JdbcVariableTableOps implements VariableTableOps {

    private final DataSourcePerTenantService dataSources;
    private final Map<String, Connection> lockConnections = new ConcurrentHashMap<>();

    JdbcVariableTableOps(DataSourcePerTenantService dataSources) {
        this.dataSources = dataSources;
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSources.retrieveDataSource());
    }

    /**
     * GET_LOCK belongs to the MySQL session that took it, and JdbcTemplate borrows a different pooled connection per
     * call. So the lock is taken on a connection of its own, held until unlock, and released on that same connection.
     */
    @Override
    public boolean tryLock(String tenant) {
        Connection connection = null;
        try {
            connection = dataSources.retrieveDataSource().getConnection();
            if (lockQuery(connection, "SELECT GET_LOCK(?, 0)", tenant) == 1) {
                lockConnections.put(tenant, connection);
                return true;
            }
            connection.close();
            return false;
        } catch (SQLException e) {
            closeQuietly(connection);
            throw new IllegalStateException("Could not take the variables retention lock for " + tenant, e);
        }
    }

    @Override
    public void unlock(String tenant) {
        Connection connection = lockConnections.remove(tenant);
        if (connection == null) {
            return;
        }
        try {
            lockQuery(connection, "SELECT RELEASE_LOCK(?)", tenant);
        } catch (SQLException e) {
            // closing the connection below still frees the lock once the pool retires it
        } finally {
            closeQuietly(connection);
        }
    }

    private static int lockQuery(Connection connection, String sql, String tenant) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, lockName(tenant));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            // nothing left to do with a connection that will not close
        }
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
