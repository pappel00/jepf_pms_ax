package com.platform.framework.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 예시:
 *
 * framework:
 *   security:
 *     jwt:
 *       secret: ${JWT_SECRET}
 *       access-token-validity-seconds: 3600
 *       refresh-token-validity-seconds: 1209600
 */
@ConfigurationProperties(prefix = "framework.security.jwt")
public class JwtProperties {

    /** 운영 환경에서는 반드시 외부 설정(환경변수/Vault 등)으로 재정의해야 한다. */
    private String secret = "CHANGE-ME-CHANGE-ME-CHANGE-ME-CHANGE-ME-32B";

    private long accessTokenValiditySeconds = 3600L;

    private long refreshTokenValiditySeconds = 1_209_600L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public void setAccessTokenValiditySeconds(long accessTokenValiditySeconds) {
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }

    public void setRefreshTokenValiditySeconds(long refreshTokenValiditySeconds) {
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }
}
