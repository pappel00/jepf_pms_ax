package com.platform.module.user.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * UserGroup - Role 매핑(N:M). roleCode 는 Role 엔티티의 PK가 아니라 Spring Security
 * GrantedAuthority 문자열과 정확히 일치해야 하는 값이다(PageRole과 동일한 설계 이유) — 이 그룹에
 * 속한 사용자는 로그인 시 이 roleCode 를 JWT roles claim 에 그대로 받는다.
 * 순수 매핑 레코드라 소프트 삭제 대신 하드 삭제로 관리한다.
 */
@Entity
@Table(name = "usr_user_group_role",
        uniqueConstraints = @UniqueConstraint(name = "uk_ugr_tenant_group_role",
                columnNames = {"tenant_id", "user_group_id", "role_code"}))
public class UserGroupRole extends TenantAwareEntity {

    @Column(name = "user_group_id", length = 64, nullable = false)
    private String userGroupId;

    @Column(name = "role_code", length = 50, nullable = false)
    private String roleCode;

    protected UserGroupRole() {
    }

    private UserGroupRole(String userGroupId, String roleCode) {
        super(PrefixedIdGenerator.generate("UGRL"));
        this.userGroupId = userGroupId;
        this.roleCode = roleCode;
    }

    public static UserGroupRole create(String userGroupId, String roleCode) {
        return new UserGroupRole(userGroupId, roleCode);
    }

    public String getUserGroupId() {
        return userGroupId;
    }

    public String getRoleCode() {
        return roleCode;
    }
}
