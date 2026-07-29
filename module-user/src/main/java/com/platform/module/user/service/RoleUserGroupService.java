package com.platform.module.user.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.user.domain.Role;
import com.platform.module.user.domain.UserGroupRole;
import com.platform.module.user.dto.RoleUserGroupDto;
import com.platform.module.user.dto.UserGroupDto;
import com.platform.module.user.dto.UserGroupRoleDto;
import com.platform.module.user.exception.UserModuleErrorCode;
import com.platform.module.user.repository.RoleRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupRoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role 관점에서 "이 역할이 매핑된 사용자그룹" 조회/추가/제거.
 * UserGroupRoleController(그룹 관점)와 저장소는 완전히 같다 — 어느 쪽에서 생성했든 같은
 * UserGroupRole 테이블에 쌓이는 동일한 매핑이다. Role은 module-user 안에 있으므로
 * PageRole(roleCode만 저장)과 달리 Role의 PK(roleId)를 그대로 쓸 수 있다.
 */
@Service
@Transactional(readOnly = true)
public class RoleUserGroupService {

    private final RoleRepository roleRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupRoleRepository userGroupRoleRepository;

    public RoleUserGroupService(RoleRepository roleRepository,
                                 UserGroupRepository userGroupRepository,
                                 UserGroupRoleRepository userGroupRoleRepository) {
        this.roleRepository = roleRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupRoleRepository = userGroupRoleRepository;
    }

    @Transactional
    public UserGroupRoleDto.Response addUserGroup(String roleId, RoleUserGroupDto.CreateRequest request) {
        Role role = findRoleOrThrow(roleId);
        userGroupRepository.findByIdAndDeletedYn(request.userGroupId(), "N")
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_GROUP_NOT_FOUND,
                        "존재하지 않는 사용자그룹입니다: " + request.userGroupId()));
        if (userGroupRoleRepository.existsByUserGroupIdAndRoleCode(request.userGroupId(), role.getRoleCode())) {
            throw new BusinessException(UserModuleErrorCode.USER_GROUP_ROLE_MAPPING_DUPLICATED,
                    "이미 매핑된 사용자그룹입니다: " + request.userGroupId());
        }
        UserGroupRole mapping = UserGroupRole.create(request.userGroupId(), role.getRoleCode());
        return UserGroupRoleDto.Response.from(userGroupRoleRepository.saveAndFlush(mapping));
    }

    public List<UserGroupDto.Response> getUserGroups(String roleId) {
        Role role = findRoleOrThrow(roleId);
        List<String> userGroupIds = userGroupRoleRepository.findAllByRoleCode(role.getRoleCode()).stream()
                .map(UserGroupRole::getUserGroupId)
                .toList();
        if (userGroupIds.isEmpty()) {
            return List.of();
        }
        return userGroupRepository.findAllById(userGroupIds).stream()
                .map(UserGroupDto.Response::from)
                .toList();
    }

    @Transactional
    public void removeUserGroup(String roleId, String mappingId) {
        findRoleOrThrow(roleId);
        UserGroupRole mapping = userGroupRoleRepository.findById(mappingId)
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_GROUP_ROLE_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + mappingId));
        userGroupRoleRepository.delete(mapping);
    }

    private Role findRoleOrThrow(String roleId) {
        return roleRepository.findByIdAndDeletedYn(roleId, "N")
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.ROLE_NOT_FOUND,
                        "존재하지 않는 역할입니다: " + roleId));
    }
}
