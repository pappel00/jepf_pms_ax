package com.platform.module.user.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.InvalidRequestException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.security.access.SystemRoles;
import com.platform.module.user.domain.UserGroupRole;
import com.platform.module.user.dto.UserGroupRoleDto;
import com.platform.module.user.exception.UserModuleErrorCode;
import com.platform.module.user.repository.RoleRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupRoleRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserGroup에 Role을 매핑(부여)한다. 사진 속 "사용자그룹 관리"/"Role 관리" 화면의 "권한"/
 * "사용자그룹" 탭 대응.
 *
 * SystemRoles(ROLE_SYSTEM_ADMIN/ROLE_ADMIN/ROLE_USER)는 프레임워크 레벨 권한이라 Role 마스터
 * 조회 없이 항상 허용하고, 그 외 값은 Role 마스터(/api/v1/roles)에 실제로 존재하는지 검증한다
 * (예전에 UserService.updateRoles()에 있던 검증 로직을 그대로 옮겨왔다 — User가 더 이상 역할을
 * 직접 갖지 않으므로, 역할 코드 검증이 실제로 일어나는 지점이 여기로 바뀌었다).
 */
@Service
@Transactional(readOnly = true)
public class UserGroupRoleService {

    private static final Set<String> ALWAYS_ALLOWED_ROLE_CODES =
            Set.of(SystemRoles.SYSTEM_ADMIN, SystemRoles.ADMIN, SystemRoles.USER);

    private final UserGroupRoleRepository userGroupRoleRepository;
    private final UserGroupRepository userGroupRepository;
    private final RoleRepository roleRepository;

    public UserGroupRoleService(UserGroupRoleRepository userGroupRoleRepository,
                                 UserGroupRepository userGroupRepository,
                                 RoleRepository roleRepository) {
        this.userGroupRoleRepository = userGroupRoleRepository;
        this.userGroupRepository = userGroupRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public UserGroupRoleDto.Response create(String userGroupId, UserGroupRoleDto.CreateRequest request) {
        ensureUserGroupExists(userGroupId);
        validateRoleCode(request.roleCode());
        if (userGroupRoleRepository.existsByUserGroupIdAndRoleCode(userGroupId, request.roleCode())) {
            throw new BusinessException(UserModuleErrorCode.USER_GROUP_ROLE_MAPPING_DUPLICATED,
                    "이미 매핑된 역할입니다: " + request.roleCode());
        }
        UserGroupRole mapping = UserGroupRole.create(userGroupId, request.roleCode());
        return UserGroupRoleDto.Response.from(userGroupRoleRepository.saveAndFlush(mapping));
    }

    public List<UserGroupRoleDto.Response> getList(String userGroupId) {
        return userGroupRoleRepository.findAllByUserGroupId(userGroupId).stream()
                .map(UserGroupRoleDto.Response::from)
                .toList();
    }

    @Transactional
    public void delete(String userGroupId, String mappingId) {
        UserGroupRole mapping = userGroupRoleRepository.findByIdAndUserGroupId(mappingId, userGroupId)
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_GROUP_ROLE_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + mappingId));
        userGroupRoleRepository.delete(mapping);
    }

    private void validateRoleCode(String roleCode) {
        if (ALWAYS_ALLOWED_ROLE_CODES.contains(roleCode)) {
            return;
        }
        if (!roleRepository.existsByRoleCodeAndDeletedYn(roleCode, "N")) {
            throw new InvalidRequestException(UserModuleErrorCode.INVALID_ROLE_CODE,
                    "존재하지 않는 역할 코드입니다: " + roleCode);
        }
    }

    private void ensureUserGroupExists(String userGroupId) {
        userGroupRepository.findByIdAndDeletedYn(userGroupId, "N")
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_GROUP_NOT_FOUND,
                        "존재하지 않는 사용자그룹입니다: " + userGroupId));
    }
}
