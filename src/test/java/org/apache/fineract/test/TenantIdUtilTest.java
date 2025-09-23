package org.apache.fineract.test;

import org.apache.fineract.config.security.utils.TenantIdUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import static org.junit.jupiter.api.Assertions.*;

class TenantIdUtilTest {

    private TenantIdUtil tenantIdUtil;

    @BeforeEach
    void setUp() {
        // Example whitelisted URIs using ant-style patterns
        String[] allowedURIs = new String[]{"/public/**", "/health", "/swagger/**"};
        PathMatcher pathMatcher = new AntPathMatcher();
        tenantIdUtil = new TenantIdUtil(allowedURIs, pathMatcher);
    }

    @Test
    @DisplayName("Should return default tenant when tenantId is blank and requestURI is whitelisted")
    void shouldUseDefaultTenantId() {
        String result = tenantIdUtil.useDefaultTenantIdIfBlank("", "/public/resource");
        assertEquals("default", result);
    }

    @Test
    @DisplayName("Should return original tenantId when it's not blank")
    void shouldReturnProvidedTenantId() {
        String result = tenantIdUtil.useDefaultTenantIdIfBlank("customTenant", "/public/resource");
        assertEquals("customTenant", result);
    }

    @Test
    @DisplayName("Should return original tenantId when URI is not whitelisted")
    void shouldNotUseDefaultForNonWhitelistedURI() {
        String result = tenantIdUtil.useDefaultTenantIdIfBlank("", "/secured/data");
        assertEquals("", result);
    }

    @Test
    @DisplayName("Should return false if requestURI is null")
    void shouldReturnFalseForNullURI() {
        String result = tenantIdUtil.useDefaultTenantIdIfBlank("", null);
        assertEquals("", result);
    }
}

