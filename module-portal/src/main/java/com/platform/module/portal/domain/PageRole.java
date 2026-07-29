package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Page - Role 매핑(N:M). 사진 속 페이지 관리 화면의 "권한" 탭 대응.
 * "이 페이지는 이 역할코드를 가진 사용자만 접근 가능하다"는 데이터 기반 인가 정보다.
 *
 * roleCode 는 Role 엔티티의 PK가 아니라 실제 Spring Security GrantedAuthority 문자열
 * (User.roles 에 담기는 값, 예: "ROLE_ADMIN" 또는 커스텀 Role의 roleCode인 "CNTN_OWNR")과
 * 정확히 일치해야 하는 값이다. module-portal 은 module-user 에 의존하지 않지만(의존 방향이
 * 반대), 인가 체크 자체는 SecurityContext 의 GrantedAuthority 문자열과 이 값을 직접
 * 비교하는 것만으로 가능하므로 module-user 를 몰라도 실제 인가 로직에 연결할 수 있다
 * (PageRoleService.isAccessible 참고).
 */
@Entity
@Table(name = "prt_page_role",
        uniqueConstraints = @UniqueConstraint(name = "uk_pr_tenant_page_role",
                columnNames = {"tenant_id", "page_id", "role_code"}))
public class PageRole extends TenantAwareEntity {

    @Column(name = "page_id", length = 64, nullable = false)
    private String pageId;

    @Column(name = "role_code", length = 50, nullable = false)
    private String roleCode;

    protected PageRole() {
    }

    private PageRole(String pageId, String roleCode) {
        super(PrefixedIdGenerator.generate("PGR"));
        this.pageId = pageId;
        this.roleCode = roleCode;
    }

    public static PageRole create(String pageId, String roleCode) {
        return new PageRole(pageId, roleCode);
    }

    public String getPageId() {
        return pageId;
    }

    public String getRoleCode() {
        return roleCode;
    }
}
