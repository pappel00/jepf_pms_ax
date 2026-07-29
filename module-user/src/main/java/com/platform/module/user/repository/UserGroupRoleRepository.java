package com.platform.module.user.repository;

import com.platform.module.user.domain.UserGroupRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRoleRepository extends JpaRepository<UserGroupRole, String> {
    boolean existsByUserGroupIdAndRoleCode(String userGroupId, String roleCode);
    List<UserGroupRole> findAllByUserGroupId(String userGroupId);
    Optional<UserGroupRole> findByIdAndUserGroupId(String id, String userGroupId);

    /** 여러 그룹에 걸쳐 매핑된 Role을 한 번에 조회 — 최종 권한(effective roles) 계산용. */
    List<UserGroupRole> findAllByUserGroupIdIn(Collection<String> userGroupIds);

    /** Role 관점의 역방향 조회 — "이 역할이 매핑된 모든 그룹" (RoleUserGroupController). */
    List<UserGroupRole> findAllByRoleCode(String roleCode);
}
