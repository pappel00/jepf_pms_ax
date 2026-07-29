package com.platform.module.user.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * UserGroup - User 매핑(N:M). "이 사용자가 이 그룹에 속해 있다"는 소속 정보다.
 * 순수 매핑 레코드라 소프트 삭제 대신 하드 삭제로 관리한다.
 */
@Entity
@Table(name = "usr_user_group_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_ugu_tenant_group_user",
                columnNames = {"tenant_id", "user_group_id", "user_id"}))
public class UserGroupUser extends TenantAwareEntity {

    @Column(name = "user_group_id", length = 64, nullable = false)
    private String userGroupId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    protected UserGroupUser() {
    }

    private UserGroupUser(String userGroupId, String userId) {
        super(PrefixedIdGenerator.generate("UGU"));
        this.userGroupId = userGroupId;
        this.userId = userId;
    }

    public static UserGroupUser create(String userGroupId, String userId) {
        return new UserGroupUser(userGroupId, userId);
    }

    public String getUserGroupId() {
        return userGroupId;
    }

    public String getUserId() {
        return userId;
    }
}
