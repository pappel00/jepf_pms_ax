package com.platform.module.tenant.event;

/**
 * 새 회사(테넌트)가 생성됐을 때 발행되는 이벤트.
 * module-tenant는 module-user(Role/UserGroup)를 모르는 구조라, 새 테넌트의 기초 데이터
 * (Role 마스터, 기본 UserGroup 등)를 직접 만들 수 없다. 대신 이 이벤트를 발행해두면,
 * module-user 쪽이 이걸 리스닝해서 알아서 부트스트랩한다 — 의존 방향을 거스르지 않으면서도
 * "회사가 생기면 자동으로 기초 데이터가 갖춰진다"를 만족시키는 방법이다.
 */
public class TenantCreatedEvent {

    private final String tenantId;
    private final String companyCode;

    public TenantCreatedEvent(String tenantId, String companyCode) {
        this.tenantId = tenantId;
        this.companyCode = companyCode;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCompanyCode() {
        return companyCode;
    }
}
