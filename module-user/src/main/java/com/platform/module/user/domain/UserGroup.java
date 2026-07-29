package com.platform.module.user.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 사용자그룹. 사진 속 BackOffice "사용자그룹 관리" 화면 대응.
 * 실제 권한 부여 흐름은 Role -> UserGroup -> User 3단계다: Role은 UserGroup에 매핑되고
 * (UserGroupRole), User는 UserGroup에 소속되며(UserGroupUser), 로그인 시 사용자가 속한
 * 모든 그룹의 Role을 합친 것이 최종 권한(JWT roles claim)이 된다.
 *
 * portalId 는 다른 참조 컬럼들과 동일하게 단순 문자열 참조다(module-user는 module-portal에
 * 의존하지 않는다).
 */
@Entity
@Table(name = "usr_user_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_ugrp_tenant_code", columnNames = {"tenant_id", "group_code"}))
public class UserGroup extends TenantAwareEntity {

    @Column(name = "group_code", length = 30, nullable = false)
    private String groupCode;

    @Column(name = "group_name", length = 200, nullable = false)
    private String groupName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "portal_id", length = 64)
    private String portalId;

    /** 신규 가입 시 자동으로 배정되는 "기본권한" 그룹인지 여부. 테넌트당 하나만 Y로 두는 것을 권장. */
    @Column(name = "default_yn", length = 1, nullable = false)
    private String defaultYn = "N";

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected UserGroup() {
    }

    private UserGroup(String groupCode, String groupName, String category, String portalId, String defaultYn) {
        super(PrefixedIdGenerator.generate("UGR"));
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.category = category;
        this.portalId = portalId;
        this.defaultYn = "Y".equals(defaultYn) ? "Y" : "N";
    }

    public static UserGroup create(String groupCode, String groupName, String category, String portalId, String defaultYn) {
        return new UserGroup(groupCode, groupName, category, portalId, defaultYn);
    }

    public void update(String groupName, String category, String portalId) {
        this.groupName = groupName;
        this.category = category;
        this.portalId = portalId;
    }

    public void markAsDefault() {
        this.defaultYn = "Y";
    }

    public void unmarkAsDefault() {
        this.defaultYn = "N";
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getCategory() {
        return category;
    }

    public String getPortalId() {
        return portalId;
    }

    public String getDefaultYn() {
        return defaultYn;
    }

    public String getUseYn() {
        return useYn;
    }
}
