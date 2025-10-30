package org.apache.fineract.test;

import org.apache.fineract.core.service.TenantDatabaseUpgradeService;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TenantDatabaseUpgradeServiceTest {
    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private Logger logger;

    @InjectMocks
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
        MockitoAnnotations.openMocks(this);

        tenantDatabaseCreator = new TenantDatabaseUpgradeService();
        tenantDatabaseCreator.username = username;
        tenantDatabaseCreator.password = password;
        tenantDatabaseCreator.hostname = hostname;
        tenantDatabaseCreator.port = port;
        tenantDatabaseCreator.jdbcProtocol = jdbcProtocol;
        tenantDatabaseCreator.jdbcSubprotocol = jdbcSubprotocol;
        tenantDatabaseCreator.tenants = tenants;

        // Mock static DriverManager
        mockStatic(DriverManager.class);

        when(DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
    }

    @AfterEach
    void tearDown() {
        clearAllCaches();
    }

    @Test
    public void shouldCreateDatabasesForAllTenants() throws Exception {
        // Given
        when(statement.executeUpdate(anyString())).thenReturn(1);

        // When
        tenantDatabaseCreator.createTenantsIfNotExists();

        // Then
        verify(statement, times(2)).executeUpdate(anyString());
        verify(logger).info("Database checked/created for tenant: {}");
        verify(logger).info("Database checked/created for tenant: {}");

        verify(statement, times(1))
                .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant1`");
        verify(statement, times(1))
                .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant2`");

        verifyNoMoreInteractions(logger);
    }

    @Test
    public void shouldHandleSQLExceptionGracefully() throws Exception {
        // Given
        doThrow(new SQLException("Test SQL error"))
                .when(statement).executeUpdate(anyString());

        // When
        tenantDatabaseCreator.createTenantsIfNotExists();

        // Then
        verify(logger, atLeastOnce()).error(startsWith("Error creating or migrating tenant database:"), anyString(), any(SQLException.class));
    }

    @Test
    public void shouldCloseResourcesAfterExecution() throws Exception {
        // Given
        when(statement.executeUpdate(anyString())).thenReturn(1);

        // When
        tenantDatabaseCreator.createTenantsIfNotExists();

        // Then
        verify(connection, times(2)).close();
        verify(statement, times(2)).close();
    }

}
