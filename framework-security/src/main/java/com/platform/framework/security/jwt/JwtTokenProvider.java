package com.platform.framework.security.jwt;

import com.platform.framework.core.common.exception.SecurityException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * MAD 4200 core/security: JWT 인증, 인가, 롤(Role) 검증 엔진의 최소 구현체.
 * 발급/파싱만 담당하며, 세부 인가(Role 매핑) 로직은 module-user/module-portal 연동 시 확장한다.
 *
 * accessToken과 refreshToken은 같은 시크릿으로 서명되지만 tokenType claim으로 구분된다 —
 * refreshToken을 실수로(혹은 악의적으로) Authorization 헤더에 넣어 access token처럼 쓰는 것을
 * JwtAuthenticationFilter가 막을 수 있게 하기 위함이다 (isRefreshToken 참고).
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userId, String tenantId, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TENANT_ID, tenantId)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getAccessTokenValiditySeconds())))
                .signWith(secretKey)
                .compact();
    }

    /** roles claim이 없다 — refresh token은 신원 확인용일 뿐, 인가에 쓰이면 안 된다. */
    public String createRefreshToken(String userId, String tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TENANT_ID, tenantId)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getRefreshTokenValiditySeconds())))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new SecurityException("ERR-AUTH-0001", "인증 토큰이 만료되었습니다.", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new SecurityException("ERR-AUTH-0002", "유효하지 않은 인증 토큰입니다.", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    public String getUserId(Claims claims) {
        return claims.getSubject();
    }

    public String getTenantId(Claims claims) {
        return claims.get(CLAIM_TENANT_ID, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        return roles != null ? roles : List.of();
    }

    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }
}
