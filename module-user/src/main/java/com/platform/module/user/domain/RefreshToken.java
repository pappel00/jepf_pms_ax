package com.platform.module.user.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 발급된 refresh token을 서버 측에서 추적한다. 순수 stateless JWT라면 자연 만료 전까지
 * 탈취된 토큰을 막을 방법이 없어서, DB에 해시를 저장해 로그아웃/재발급 시 즉시 무효화(revoke)할
 * 수 있게 한다. 토큰 원문이 아니라 SHA-256 해시만 저장한다 — DB가 노출돼도 토큰 원문을
 * 복원할 수 없다 (비밀번호를 해시로 저장하는 것과 같은 이유).
 *
 * refresh 때마다 기존 레코드를 revoke하고 새 레코드를 발급하는 "rotation" 방식을 쓴다
 * (AuthService.refresh() 참고) — 탈취된 refresh token이 재사용되는 것을 막기 위함이다.
 */
@Entity
@Table(name = "usr_refresh_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = "token_hash"))
public class RefreshToken extends TenantAwareEntity {

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_yn", length = 1, nullable = false)
    private String revokedYn = "N";

    protected RefreshToken() {
    }

    private RefreshToken(String userId, String tokenHash, LocalDateTime expiresAt) {
        super(PrefixedIdGenerator.generate("RTK"));
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(String userId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(userId, tokenHash, expiresAt);
    }

    public void revoke() {
        this.revokedYn = "Y";
    }

    public boolean isRevoked() {
        return "Y".equals(revokedYn);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public String getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getRevokedYn() {
        return revokedYn;
    }
}
