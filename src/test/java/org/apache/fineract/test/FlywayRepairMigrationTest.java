package org.apache.fineract.test;

import com.googlecode.flyway.core.Flyway;
import org.apache.fineract.core.service.DataSourcePerTenantService;
import org.apache.fineract.core.service.TenantDatabaseUpgradeService;
import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the MySQL 8.0 upgrade change: {@code fw.repair()} is conditionally
 * called before {@code fw.migrate()} in the tenant Flyway migration flow,
 * controlled by the {@code fineract.flyway.repair-on-startup} property.
 * <p>
 * This is needed because migration files V22, V47, and V48 were modified
 * (backtick-escaping the 'grouping' keyword), which changes their Flyway
 * checksums. {@code repair()} updates stored checksums to match, preventing
 * checksum mismatch errors on existing databases.
 */
class FlywayRepairMigrationTest {

    private TenantDatabaseUpgradeService service;
    private TenantServerConnectionRepository repository;
    private Connection mockConn;
    private Statement mockStmt;

    @BeforeEach
    void setUp() throws Exception {
        service = Mockito.spy(new TenantDatabaseUpgradeService());

        repository = mock(TenantServerConnectionRepository.class);
        DataSourcePerTenantService dataSourcePerTenantService = mock(DataSourcePerTenantService.class);
        DataSource dataSource = mock(DataSource.class);
        mockConn = mock(Connection.class);
        mockStmt = mock(Statement.class);

        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "dataSourcePerTenantService", dataSourcePerTenantService);
        ReflectionTestUtils.setField(service, "hostname", "localhost");
        ReflectionTestUtils.setField(service, "port", 3306);
        ReflectionTestUtils.setField(service, "username", "root");
        ReflectionTestUtils.setField(service, "password", "pass");
        ReflectionTestUtils.setField(service, "jdbcProtocol", "jdbc");
        ReflectionTestUtils.setField(service, "jdbcSubprotocol", "mysql");
        ReflectionTestUtils.setField(service, "driverClass", "com.mysql.cj.jdbc.Driver");
        ReflectionTestUtils.setField(service, "userTokenAccessValiditySeconds", "600");
        ReflectionTestUtils.setField(service, "userTokenRefreshValiditySeconds", "43200");
        ReflectionTestUtils.setField(service, "clientAccessTokenValidity", "3600");
        ReflectionTestUtils.setField(service, "channelClientSecret", "secret");
        ReflectionTestUtils.setField(service, "flywayRepairOnStartup", true);
        ReflectionTestUtils.setField(service, "tenants", Arrays.asList("oaf"));

        when(dataSourcePerTenantService.retrieveDataSource()).thenReturn(dataSource);
        when(mockConn.createStatement()).thenReturn(mockStmt);
        Mockito.doReturn(mockConn)
                .when(service)
                .createConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("repair() is called before migrate() when flywayRepairOnStartup is true")
    void shouldCallRepairBeforeMigrate() throws Exception {
        // Arrange
        TenantServerConnection tenant = new TenantServerConnection();
        tenant.setSchemaName("oaf");
        tenant.setAutoUpdateEnabled(true);
        when(repository.findAll()).thenReturn(Collections.singletonList(tenant));
        when(repository.findOneBySchemaName("oaf")).thenReturn(tenant);
        when(mockStmt.executeUpdate(Mockito.anyString())).thenReturn(1);

        // Act
        try (MockedConstruction<Flyway> flywayMock = Mockito.mockConstruction(Flyway.class, (mock, ctx) ->
            when(mock.migrate()).thenReturn(0)
        )) {
            service.setupEnvironment();

            assertEquals(2, flywayMock.constructed().size(),
                    "Expected 2 Flyway instances: one for core schema, one for tenant");

            Flyway tenantFlyway = flywayMock.constructed().get(1);
            verify(tenantFlyway, times(1)).repair();
            verify(tenantFlyway, times(1)).migrate();

            InOrder inOrder = Mockito.inOrder(tenantFlyway);
            inOrder.verify(tenantFlyway).repair();
            inOrder.verify(tenantFlyway).migrate();
        }
    }

    @Test
    @DisplayName("repair() is NOT called when flywayRepairOnStartup is false")
    void shouldNotCallRepairWhenDisabled() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(service, "flywayRepairOnStartup", false);

        TenantServerConnection tenant = new TenantServerConnection();
        tenant.setSchemaName("oaf");
        tenant.setAutoUpdateEnabled(true);
        when(repository.findAll()).thenReturn(Collections.singletonList(tenant));
        when(repository.findOneBySchemaName("oaf")).thenReturn(tenant);
        when(mockStmt.executeUpdate(Mockito.anyString())).thenReturn(1);

        // Act
        try (MockedConstruction<Flyway> flywayMock = Mockito.mockConstruction(Flyway.class, (mock, ctx) ->
            when(mock.migrate()).thenReturn(0)
        )) {
            service.setupEnvironment();

            Flyway tenantFw = flywayMock.constructed().get(1);
            verify(tenantFw, never()).repair();
            verify(tenantFw, times(1)).migrate();
        }
    }

    @Test
    @DisplayName("repair() is NOT called for the core/default schema Flyway migration")
    void shouldNotCallRepairForDefaultSchema() throws Exception {
        // Arrange
        TenantServerConnection tenant = new TenantServerConnection();
        tenant.setSchemaName("oaf");
        tenant.setAutoUpdateEnabled(true);
        when(repository.findAll()).thenReturn(Collections.singletonList(tenant));
        when(repository.findOneBySchemaName("oaf")).thenReturn(tenant);
        when(mockStmt.executeUpdate(Mockito.anyString())).thenReturn(1);

        // Act
        try (MockedConstruction<Flyway> flywayMock = Mockito.mockConstruction(Flyway.class, (mock, ctx) ->
            when(mock.migrate()).thenReturn(0)
        )) {
            service.setupEnvironment();

            Flyway coreFlyway = flywayMock.constructed().get(0);
            verify(coreFlyway, never()).repair();
            verify(coreFlyway, times(1)).migrate();
        }
    }
}
