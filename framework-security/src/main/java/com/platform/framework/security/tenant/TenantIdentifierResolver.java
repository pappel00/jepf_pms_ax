package com.platform.framework.security.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Hibernate가 현재 스레드의 테넌트ID를 물어볼 때 {@link TenantContext} 값을 반환한다.
 * TenantContext가 비어있는 요청(예: 로그인 API 등 아직 테넌트가 정해지지 않은 경우)은
 * DEFAULT_TENANT로 처리한다.
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    public static final String DEFAULT_TENANT = "SYSTEM";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
