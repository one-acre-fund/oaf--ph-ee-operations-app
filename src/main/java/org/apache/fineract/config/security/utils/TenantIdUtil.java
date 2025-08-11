package org.apache.fineract.config.security.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;

@Component
public class TenantIdUtil {

    private static final String DEFAULT_TENANT_ID = "default";
    private final String[] allowedURIs;

    private final PathMatcher pathMatcher;

    public TenantIdUtil(@Value("${spring.security.whitelist}") String[] allowedURIs, PathMatcher pathMatcher) {
        this.allowedURIs = allowedURIs;
        this.pathMatcher = pathMatcher;
    }

    /**
     * Gets the default tenant identifier if it is allowed for a particular URI
     *
     * @param tenantId
     *            the existing tenant id
     * @param requestURI
     *            the request URI to be used in determining if default ID should be used
     * @return tenant identifier
     */
    public String useDefaultTenantIdIfBlank(String tenantId, String requestURI) {
        if (StringUtils.isBlank(tenantId) && canUseDefaultTenant(requestURI)) {
            return DEFAULT_TENANT_ID;
        }
        return tenantId;
    }

    private boolean canUseDefaultTenant(String requestURI) {
        if (requestURI == null) {
            return false;
        }
        for (String uri : allowedURIs) {
            if (pathMatcher.match(uri, requestURI)) {
                return true;
            }
        }
        return false;
    }
}
