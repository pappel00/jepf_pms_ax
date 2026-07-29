package com.platform.framework.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final JwtProperties properties = new JwtProperties();
    private final JwtTokenProvider provider = new JwtTokenProvider(properties);

    @Test
    void createAndParseToken() {
        String token = provider.createAccessToken("user-001", "tenant-001", List.of("ROLE_USER"));

        assertThat(provider.validateToken(token)).isTrue();

        Claims claims = provider.parseClaims(token);
        assertThat(provider.getUserId(claims)).isEqualTo("user-001");
        assertThat(provider.getTenantId(claims)).isEqualTo("tenant-001");
        assertThat(provider.getRoles(claims)).containsExactly("ROLE_USER");
    }

    @Test
    void invalidTokenFailsValidation() {
        assertThat(provider.validateToken("not-a-valid-token")).isFalse();
    }
}
