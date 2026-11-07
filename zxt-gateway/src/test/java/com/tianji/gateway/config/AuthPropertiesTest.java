package com.tianji.gateway.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthPropertiesTest {

    @Test
    void initializesWithOnlyDefaultPublicPathsWhenConfigurationIsMissing() {
        AuthProperties properties = new AuthProperties();

        assertDoesNotThrow(properties::afterPropertiesSet);

        assertEquals(Set.of("/error/**", "/jwks", "/accounts/login",
                "/accounts/admin/login", "/accounts/refresh"), properties.getExcludePath());
    }

    @Test
    void retainsConfiguredPublicPathsAlongsideDefaults() throws Exception {
        AuthProperties properties = new AuthProperties();
        properties.setExcludePath(new HashSet<>(Set.of("/public/course")));

        properties.afterPropertiesSet();

        assertEquals(Set.of("/public/course", "/error/**", "/jwks", "/accounts/login",
                "/accounts/admin/login", "/accounts/refresh"), properties.getExcludePath());
    }
}
