package org.apache.fineract.export;

import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.springframework.stereotype.Component;

/**
 * Runs background work against one tenant's database. Without a tenant on the thread, queries silently go to the
 * core "tenants" schema, so the tenant is always set here and always cleared afterwards.
 */
@Component
public class TenantContext {

    private final TenantServerConnectionRepository tenantRepository;

    public TenantContext(TenantServerConnectionRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public void runAs(String tenantSchema, Runnable work) {
        TenantServerConnection tenant = tenantRepository.findOneBySchemaName(tenantSchema);
        if (tenant == null) {
            throw new IllegalStateException("Unknown tenant " + tenantSchema);
        }
        ThreadLocalContextUtil.setTenant(tenant);
        try {
            work.run();
        } finally {
            ThreadLocalContextUtil.clear();
        }
    }
}
