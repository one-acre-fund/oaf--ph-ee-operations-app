package org.apache.fineract.export;

import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantContextTest {

    private final TenantServerConnectionRepository repository = mock(TenantServerConnectionRepository.class);
    private final TenantContext context = new TenantContext(repository);

    @AfterEach
    void clear() {
        ThreadLocalContextUtil.clear();
    }

    @Test
    @DisplayName("Sets the tenant for the work and clears it afterwards, also when the work fails")
    void setsAndClearsTenant() {
        TenantServerConnection oaf = new TenantServerConnection();
        when(repository.findOneBySchemaName("oaf")).thenReturn(oaf);
        AtomicReference<TenantServerConnection> seen = new AtomicReference<>();

        context.runAs("oaf", () -> seen.set(ThreadLocalContextUtil.getTenant()));
        assertThrows(IllegalStateException.class, () -> context.runAs("oaf", () -> {
            throw new IllegalStateException("boom");
        }));

        assertSame(oaf, seen.get());
        assertNull(ThreadLocalContextUtil.getTenant());
    }

    @Test
    @DisplayName("Refuses an unknown tenant instead of falling back to the core schema")
    void unknownTenant() {
        assertThrows(IllegalStateException.class, () -> context.runAs("nope", () -> { }));
        assertNull(ThreadLocalContextUtil.getTenant());
    }
}
