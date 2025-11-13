package org.apache.fineract.test;

import org.apache.fineract.core.service.TenantDatabaseUpgradeService;
import org.apache.fineract.core.service.DataSourcePerTenantService;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantDatabaseUpgradeTest {
    private Connection connection;
    private Statement statement;
    private TenantDatabaseUpgradeService tenantDatabaseCreator;

    private final String username = "root";
    private final String password = "password";
    private final String hostname = "localhost";
    private final int port = 3306;
    private final String jdbcProtocol = "jdbc";
    private final String jdbcSubprotocol = "mysql";
    private final List<String> tenants = Arrays.asList("tenant1", "tenant2");

    @BeforeEach
    void setUp() throws Exception {
        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(Statement.class);

        TenantDatabaseUpgradeService realService = new TenantDatabaseUpgradeService();
        realService.username = username;
        realService.password = password;
        realService.hostname = hostname;
        realService.port = port;
        realService.jdbcProtocol = jdbcProtocol;
        realService.jdbcSubprotocol = jdbcSubprotocol;
        realService.tenants = tenants;

        tenantDatabaseCreator = Mockito.spy(realService);
        when(connection.createStatement()).thenReturn(statement);
        Mockito.doReturn(connection)
                .when(tenantDatabaseCreator)
                .createConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("Creates databases for all valid tenants and closes resources")
    void shouldCreateDatabasesForAllTenants() throws Exception {
        when(statement.executeUpdate(anyString())).thenReturn(1);
        tenantDatabaseCreator.createTenantsIfNotExists();
        verify(statement, times(2)).executeUpdate(anyString());
        verify(statement, times(1))
                .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant1`");
        verify(statement, times(1))
                .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant2`");
        verify(statement, times(2)).close();
        verify(connection, times(2)).close();
    }

    @Test
    @DisplayName("Handles SQLException in executeUpdate and still closes resources")
    void shouldHandleSQLExceptionGracefully() throws Exception {
        when(statement.executeUpdate(anyString())).thenThrow(new SQLException("Test SQL error"));
        tenantDatabaseCreator.createTenantsIfNotExists();
        verify(statement, times(2)).close();
        verify(connection, times(2)).close();
    }

    @Test
    @DisplayName("Returns early when tenants list is null")
    void shouldNotRunWhenTenantsIsNull() throws Exception {
        tenantDatabaseCreator.tenants = null;
        tenantDatabaseCreator.createTenantsIfNotExists();
        Mockito.verify(tenantDatabaseCreator, Mockito.never())
                .createConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verifyNoInteractions(statement);
        Mockito.verifyNoInteractions(connection);
    }

    @Test
    @DisplayName("Uses expected JDBC URL when creating connections")
    void shouldUseExpectedJdbcUrlWhenCreatingConnection() throws Exception {
        when(statement.executeUpdate(anyString())).thenReturn(1);
        String expectedUrl = jdbcProtocol + ":" + jdbcSubprotocol + "://" + hostname + ":" + port + "/mysql";
        tenantDatabaseCreator.createTenantsIfNotExists();
        Mockito.verify(tenantDatabaseCreator, times(2))
                .createConnection(expectedUrl, username, password);
    }

    @Test
    @DisplayName("Skips null, empty, and invalid tenant identifiers")
    void shouldSkipNullAndInvalidTenantIdentifiers() throws Exception {
        tenantDatabaseCreator.tenants = Arrays.asList("tenant1", null, "", "bad-name!", "tenant2");
        when(statement.executeUpdate(anyString())).thenReturn(1);
        tenantDatabaseCreator.createTenantsIfNotExists();
        verify(statement, times(2)).executeUpdate(anyString());
        verify(statement, times(1))
                .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant1`");
        verify(statement, times(1))
                .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant2`");
    }

    @Test
    @DisplayName("Trims tenant identifiers and skips empty after trim")
    void shouldTrimTenantIdentifiersBeforeUse() throws Exception {
        tenantDatabaseCreator.tenants = Arrays.asList("  tenant1  ", "   "); // second becomes empty after trim
        when(statement.executeUpdate(anyString())).thenReturn(1);
        tenantDatabaseCreator.createTenantsIfNotExists();
        verify(statement, times(1))
                .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant1`");
        verify(statement, times(1)).close();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("Returns early when tenants list is empty")
    void shouldReturnEarlyWhenTenantsEmpty() throws Exception {
        tenantDatabaseCreator.tenants = Arrays.asList();
        tenantDatabaseCreator.createTenantsIfNotExists();
        Mockito.verify(tenantDatabaseCreator, Mockito.never())
                .createConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("setupEnvironment invokes createTenantsIfNotExists")
    void shouldCallCreateTenantsIfNotExistsInSetupEnvironment() {
        TenantServerConnectionRepository repo = Mockito.mock(TenantServerConnectionRepository.class);
        when(repo.findAll()).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(tenantDatabaseCreator, "repository", repo);

        DataSourcePerTenantService dsService = Mockito.mock(DataSourcePerTenantService.class);
        DataSource ds = Mockito.mock(DataSource.class);
        when(dsService.retrieveDataSource()).thenReturn(ds);
        ReflectionTestUtils.setField(tenantDatabaseCreator, "dataSourcePerTenantService", dsService);

        Mockito.doNothing().when(tenantDatabaseCreator).createTenantsIfNotExists();
        try (MockedConstruction<com.googlecode.flyway.core.Flyway> flywayMock = Mockito.mockConstruction(com.googlecode.flyway.core.Flyway.class, (mock, ctx) -> {
            Mockito.when(mock.migrate()).thenReturn(0);
        })) {
            tenantDatabaseCreator.setupEnvironment();
            Mockito.verify(tenantDatabaseCreator, times(1)).createTenantsIfNotExists();
        }
    }
}