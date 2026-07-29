package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 어플리케이션(Portal 하위의 메뉴 그룹). 사진 속 BackOffice "어플리케이션 관리" 화면 대응.
 * portalId 는 진짜 JPA 연관관계가 아니라 Department.parentDepartmentId 와 동일한 방식의
 * 단순 참조 컬럼이다 (N+1/캐스케이드 복잡도 회피, 이 프레임워크 전반의 일관된 방식).
 */
@Entity
@Table(name = "prt_application",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_tenant_code", columnNames = {"tenant_id", "application_code"}))
public class Application extends TenantAwareEntity {

    @Column(name = "application_code", length = 30, nullable = false)
    private String applicationCode;

    @Column(name = "application_name", length = 200, nullable = false)
    private String applicationName;

    @Column(name = "portal_id", length = 64, nullable = false)
    private String portalId;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected Application() {
    }

    private Application(String applicationCode, String applicationName, String portalId) {
        super(PrefixedIdGenerator.generate("APP"));
        this.applicationCode = applicationCode;
        this.applicationName = applicationName;
        this.portalId = portalId;
    }

    public static Application create(String applicationCode, String applicationName, String portalId) {
        return new Application(applicationCode, applicationName, portalId);
    }

    public void update(String applicationName) {
        this.applicationName = applicationName;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getPortalId() {
        return portalId;
    }

    public String getUseYn() {
        return useYn;
    }
}
