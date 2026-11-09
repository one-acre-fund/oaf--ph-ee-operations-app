package org.apache.fineract.retention;

import org.apache.fineract.core.service.DataSourcePerTenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcVariableTableOpsTest {

    private final Connection connection = mock(Connection.class);
    private final Statement statement = mock(Statement.class);
    private final PreparedStatement prepared = mock(PreparedStatement.class);
    private final ResultSet rs = mock(ResultSet.class);
    private JdbcVariableTableOps ops;

    @BeforeEach
    void setUp() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        DataSourcePerTenantService dataSources = mock(DataSourcePerTenantService.class);
        when(dataSources.retrieveDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(prepared);
        when(statement.executeQuery(anyString())).thenReturn(rs);
        when(prepared.executeQuery()).thenReturn(rs);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(rs.getMetaData()).thenReturn(meta);
        ops = new JdbcVariableTableOps(dataSources);
    }

    @Test
    @DisplayName("Takes and releases a per-tenant named lock")
    void lock() throws SQLException {
        when(rs.next()).thenReturn(true, false, true, false, true, false);
        when(rs.getInt(1)).thenReturn(1, 0, 1);

        assertTrue(ops.tryLock("kenya"));
        assertFalse(ops.tryLock("kenya"));
        ops.unlock("kenya");

        verify(connection, times(2)).prepareStatement("SELECT GET_LOCK(?, 0)");
        verify(connection).prepareStatement("SELECT RELEASE_LOCK(?)");
        verify(prepared, times(3)).setString(1, "variables_retention_kenya");
    }

    @Test
    @DisplayName("Lists the partitions of the current schema in order")
    void partitionNames() throws SQLException {
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString(1)).thenReturn("p2026_11", "pmax");

        assertEquals(Arrays.asList("p2026_11", "pmax"), ops.partitionNames());
    }

    @Test
    @DisplayName("Counts one partition")
    void rowCount() throws SQLException {
        when(rs.next()).thenReturn(true, false);
        when(rs.getLong(1)).thenReturn(42L);

        assertEquals(42L, ops.rowCount("p2026_11"));
        verify(statement).executeQuery("SELECT COUNT(*) FROM variables PARTITION (p2026_11)");
    }

    @Test
    @DisplayName("Splits pmax to add a month")
    void addMonth() throws SQLException {
        ops.addMonth(YearMonth.of(2027, 4));

        verify(statement).execute("ALTER TABLE variables REORGANIZE PARTITION pmax INTO ("
                + "PARTITION p2027_04 VALUES LESS THAN (1809129600000), PARTITION pmax VALUES LESS THAN MAXVALUE)");
    }

    @Test
    @DisplayName("Drops a monthly partition")
    void dropPartition() throws SQLException {
        ops.dropPartition("p2025_11");

        verify(statement).execute("ALTER TABLE variables DROP PARTITION p2025_11");
    }

    @Test
    @DisplayName("Never puts anything but a partition name into SQL")
    void rejectsOtherNames() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> ops.dropPartition("p2025_11; DROP TABLE variables"));
        assertThrows(IllegalArgumentException.class, () -> ops.rowCount("p1) UNION SELECT 1 --"));

        verify(statement, never()).execute(anyString());
        verify(statement, never()).executeQuery(anyString());
    }
}
