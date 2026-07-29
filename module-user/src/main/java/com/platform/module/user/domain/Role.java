package com.platform.module.user.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 테넌트별 역할(권한 그룹) 마스터. 사진 속 BackOffice의 "Role 관리" 화면(역할 분류/역할ID/역할명/역할별칭)에 대응한다.
 *
 * User.roles 는 이 Role의 roleCode 값을 그대로 문자열로 담는다(느슨한 참조). JWT의 roles claim,
 * Spring Security의 GrantedAuthority 와 바로 연결되는 단순 구조를 유지하기 위해 굳이 FK로
 * 엮지 않았다 — Role은 "이 테넌트에서 어떤 역할 코드들이 유효한지"를 관리하는 마스터 데이터 역할만 한다.
 */
@Entity
@Table(name = "usr_role",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_tenant_code", columnNames = {"tenant_id", "role_code"}))
public class Role extends TenantAwareEntity {

    @Column(name = "role_code", length = 50, nullable = false)
    private String roleCode;

    @Column(name = "role_name", length = 100, nullable = false)
    private String roleName;

    @Column(name = "role_alias", length = 50)
    private String roleAlias;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected Role() {
    }

    private Role(String roleCode, String roleName, String roleAlias, String category, String description) {
        super(PrefixedIdGenerator.generate("ROL"));
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.roleAlias = roleAlias;
        this.category = category;
        this.description = description;
    }

    public static Role create(String roleCode, String roleName, String roleAlias, String category, String description) {
        return new Role(roleCode, roleName, roleAlias, category, description);
    }

    public void update(String roleName, String roleAlias, String category, String description) {
        this.roleName = roleName;
        this.roleAlias = roleAlias;
        this.category = category;
        this.description = description;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getRoleAlias() {
        return roleAlias;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
