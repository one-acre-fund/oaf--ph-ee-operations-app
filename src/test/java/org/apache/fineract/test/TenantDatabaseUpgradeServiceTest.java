package org.apache.fineract.test;

import org.apache.fineract.core.service.TenantDatabaseUpgradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantDatabaseUpgradeServiceTest {

    private TenantDatabaseUpgradeService service;
    private Connection mockConn;
    private Statement mockStmt;

    @BeforeEach
    void setUp() throws Exception {
        service = Mockito.spy(new TenantDatabaseUpgradeService());
        mockConn = Mockito.mock(Connection.class);
        mockStmt = Mockito.mock(Statement.class);

        ReflectionTestUtils.setField(service, "jdbcProtocol", "jdbc");
        ReflectionTestUtils.setField(service, "jdbcSubprotocol", "mysql");
        ReflectionTestUtils.setField(service, "hostname", "localhost");
        ReflectionTestUtils.setField(service, "port", 3306);
        ReflectionTestUtils.setField(service, "username", "user");
        ReflectionTestUtils.setField(service, "password", "pass");
        ReflectionTestUtils.setField(service, "tenants", Collections.singletonList("tenant1"));
        ReflectionTestUtils.setField(service, "flywayRepairOnStartup", false);

        when(mockConn.createStatement()).thenReturn(mockStmt);
        Mockito.doReturn(mockConn)
                .when(service)
                .createConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testCreateTenantsIfNotExists_success() throws Exception {
        when(mockStmt.executeUpdate(anyString())).thenReturn(1);

        service.createTenantsIfNotExists();

        verify(mockStmt).executeUpdate(startsWith("CREATE DATABASE IF NOT EXISTS"));
    }


    @Test
    void testCreateTenantsIfNotExists_EmptyTenantsList() {
        ReflectionTestUtils.setField(service, "tenants", Collections.emptyList());
        service.createTenantsIfNotExists();
    }
}
