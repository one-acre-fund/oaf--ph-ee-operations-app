package org.apache.fineract.test;

import org.apache.fineract.core.service.TenantDatabaseUpgradeService;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearAllCaches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.slf4j.LoggerFactory;

public class TenantDatabaseUpgradeServiceTest {
    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @InjectMocks
    private TenantDatabaseUpgradeService tenantDatabaseCreator;

    private AutoCloseable mocks;

    private final String username = "root";
    private final String password = "password";
    private final String hostname = "localhost";
    private final int port = 3306;
    private final String jdbcProtocol = "jdbc";
    private final String jdbcSubprotocol = "mysql";
    private final List<String> tenants = Arrays.asList("tenant1", "tenant2");

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        tenantDatabaseCreator.username = username;
        tenantDatabaseCreator.password = password;
        tenantDatabaseCreator.hostname = hostname;
        tenantDatabaseCreator.port = port;
        tenantDatabaseCreator.jdbcProtocol = jdbcProtocol;
        tenantDatabaseCreator.jdbcSubprotocol = jdbcSubprotocol;
        tenantDatabaseCreator.tenants = tenants;
    }

    @After
    public void tearDown() throws Exception {
        clearAllCaches();
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void shouldCreateDatabasesForAllTenants() throws Exception {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeUpdate(anyString())).thenReturn(1);
            tenantDatabaseCreator.createTenantsIfNotExists();
            verify(statement, times(2)).executeUpdate(anyString());
            verify(statement, times(1))
                    .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant1`");
            verify(statement, times(1))
                    .executeUpdate("CREATE DATABASE IF NOT EXISTS `tenant2`");
        }
    }

    @Test
    public void shouldHandleSQLExceptionGracefully() throws Exception {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            doThrow(new SQLException("Test SQL error"))
                .when(statement).executeUpdate(anyString());
            tenantDatabaseCreator.createTenantsIfNotExists();
        }
    }

    @Test
    public void shouldCloseResourcesAfterExecution() throws Exception {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeUpdate(anyString())).thenReturn(1);
            tenantDatabaseCreator.createTenantsIfNotExists();
            verify(connection, times(2)).close();
            verify(statement, times(2)).close();
        }
    }

    @Test
    public void shouldLogErrorWhenSQLExceptionOccurs() throws Exception {
        Logger mockLogger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(() -> LoggerFactory.getLogger(TenantDatabaseUpgradeService.class)).thenReturn(mockLogger);
            try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
                driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                        .thenReturn(connection);
                when(connection.createStatement()).thenReturn(statement);
                doThrow(new SQLException("Test SQL error"))
                    .when(statement).executeUpdate(anyString());
                TenantDatabaseUpgradeService service = new TenantDatabaseUpgradeService();
                service.username = username;
                service.password = password;
                service.hostname = hostname;
                service.port = port;
                service.jdbcProtocol = jdbcProtocol;
                service.jdbcSubprotocol = jdbcSubprotocol;
                service.tenants = tenants;
                service.createTenantsIfNotExists();
                for (String tenant : tenants) {
                    verify(mockLogger).error("Error creating or migrating tenant database: {}", tenant, new SQLException("Test SQL error"));
                }
            }
        }
    }
}
