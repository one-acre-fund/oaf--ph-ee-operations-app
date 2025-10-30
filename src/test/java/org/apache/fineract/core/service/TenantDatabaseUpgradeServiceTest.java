package org.apache.fineract.core.service;

import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantDatabaseUpgradeServiceTest {

    @Mock
    private TenantServerConnectionRepository repository;

    @Mock
    private DataSourcePerTenantService dataSourcePerTenantService;

    @Mock
    private Logger logger;

    @InjectMocks
    private TenantDatabaseUpgradeService service;

    @BeforeEach
    void setUp() {

        service = new TenantDatabaseUpgradeService();
        service.username = "root";
        service.password = "password";
        service.hostname = "localhost";
        service.port = 3306;
        service.jdbcProtocol = "jdbc";
        service.jdbcSubprotocol = "mysql";
    }

    @Test
    void testCreateTenantsIfNotExists_success() throws Exception {
        Connection mockConn = mock(Connection.class);
        Statement mockStmt = mock(Statement.class);

        ReflectionTestUtils.setField(service, "jdbcProtocol", "jdbc");
        ReflectionTestUtils.setField(service, "jdbcSubprotocol", "mysql");
        ReflectionTestUtils.setField(service, "hostname", "localhost");
        ReflectionTestUtils.setField(service, "port", 3306);
        ReflectionTestUtils.setField(service, "username", "user");
        ReflectionTestUtils.setField(service, "password", "pass");
        ReflectionTestUtils.setField(service, "tenants", Collections.singletonList("tenant1"));

        when(mockConn.createStatement()).thenReturn(mockStmt);
        try (MockedStatic driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString())).thenReturn(mockConn);

            service.createTenantsIfNotExists();

            verify(mockStmt).executeUpdate(startsWith("CREATE DATABASE IF NOT EXISTS"));
        }
    }


    @Test
    void testCreateTenantsIfNotExists_EmptyTenantsList() {
        ReflectionTestUtils.setField(service, "tenants", Collections.emptyList());
        service.createTenantsIfNotExists();
    }
}
