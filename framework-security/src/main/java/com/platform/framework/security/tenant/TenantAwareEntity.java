package com.platform.framework.security.tenant;

import com.platform.framework.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.TenantId;

/**
 * 테넌트(Company)에 종속된 모든 데이터의 공통 상위 클래스.
 *
 * Hibernate 6+ 의 {@code @TenantId} 파티션 방식 멀티테넌시를 사용한다:
 *  - INSERT 시 현재 테넌트ID가 자동으로 채워진다.
 *  - SELECT/UPDATE/DELETE 시 "tenant_id = 현재테넌트" 조건이 Hibernate에 의해 자동으로 추가된다.
 *  - 현재 테넌트ID는 {@link TenantIdentifierResolver} 가 {@link TenantContext} 에서 읽어온다.
 *
 * ※ Company(테넌트 자신을 정의하는 마스터 엔티티)는 이 클래스를 상속하지 않는다 — Company는
 *   "어떤 테넌트에 속하는" 데이터가 아니라 테넌트 그 자체이기 때문이다.
 */
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false, updatable = false)
    protected String tenantId;

    protected TenantAwareEntity() {
    }

    protected TenantAwareEntity(String id) {
        super(id);
    }

    public String getTenantId() {
        return tenantId;
    }
}
