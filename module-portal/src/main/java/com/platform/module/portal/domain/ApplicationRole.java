package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Application - Role 매핑. 어플리케이션 1 : Role N — 하나의 어플리케이션에 여러 Role을
 * 부여할 수 있다(어플리케이션 관점에서 "이 어플리케이션은 여러 권한 중 하나만 있어도 접근
 * 가능하다"). PageRole과 동일한 패턴이되, Page는 Role과 완전한 N:M(여러 페이지가 같은 Role을
 * 공유)인 반면 이 매핑도 구조적으로는 같은 테이블 모양이다 — "1:N"은 어플리케이션 관리 화면
 * 관점에서 "하나의 어플리케이션에 여러 Role을 매핑한다"는 요구사항을 가리킨다.
 *
 * roleCode 는 Role 엔티티의 PK가 아니라 실제 Spring Security GrantedAuthority 문자열
 * (User.roles 에 담기는 값, 예: "ROLE_ADMIN" 또는 커스텀 Role의 roleCode)과 정확히 일치해야
 * 하는 값이다. module-portal 은 module-user 에 의존하지 않지만, 인가 체크 자체는
 * SecurityContext 의 GrantedAuthority 문자열과 이 값을 직접 비교하는 것만으로 가능하므로
 * module-user 를 몰라도 실제 인가 로직에 연결할 수 있다(ApplicationRoleService.isAccessible 참고).
 */
@Entity
@Table(name = "prt_application_role",
        uniqueConstraints = @UniqueConstraint(name = "uk_apr_tenant_app_role",
                columnNames = {"tenant_id", "application_id", "role_code"}))
public class ApplicationRole extends TenantAwareEntity {

    @Column(name = "application_id", length = 64, nullable = false)
    private String applicationId;

    @Column(name = "role_code", length = 50, nullable = false)
    private String roleCode;

    protected ApplicationRole() {
    }

    private ApplicationRole(String applicationId, String roleCode) {
        super(PrefixedIdGenerator.generate("APR"));
        this.applicationId = applicationId;
        this.roleCode = roleCode;
    }

    public static ApplicationRole create(String applicationId, String roleCode) {
        return new ApplicationRole(applicationId, roleCode);
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getRoleCode() {
        return roleCode;
    }
}
