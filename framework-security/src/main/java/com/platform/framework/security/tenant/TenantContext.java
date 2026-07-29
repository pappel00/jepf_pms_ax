package com.platform.framework.security.tenant;

/**
 * MAD 4200 core/tenant: Multi-Tenancy 식별 인터셉터 (회사/부서 컨텍스트).
 * 요청 스레드 동안 현재 테넌트 ID를 보관한다. 반드시 요청 종료 시 clear() 를 호출해야 한다.
 * (JwtAuthenticationFilter 에서 finally 블록으로 처리)
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
