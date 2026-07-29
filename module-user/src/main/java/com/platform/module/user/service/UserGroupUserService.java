package com.platform.module.user.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.user.domain.User;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.dto.UserDto;
import com.platform.module.user.dto.UserGroupUserDto;
import com.platform.module.user.exception.UserModuleErrorCode;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserGroup에 User를 소속시킨다(추가/조회/제거). 사진 속 "사용자그룹 관리" 화면의 "사용자" 탭 대응.
 */
@Service
@Transactional(readOnly = true)
public class UserGroupUserService {

    private final UserGroupUserRepository userGroupUserRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final EffectiveRoleService effectiveRoleService;

    public UserGroupUserService(UserGroupUserRepository userGroupUserRepository,
                                 UserGroupRepository userGroupRepository,
                                 UserRepository userRepository,
                                 EffectiveRoleService effectiveRoleService) {
        this.userGroupUserRepository = userGroupUserRepository;
        this.userGroupRepository = userGroupRepository;
        this.userRepository = userRepository;
        this.effectiveRoleService = effectiveRoleService;
    }

    @Transactional
    public UserDto.Response addUser(String userGroupId, UserGroupUserDto.AssignRequest request) {
        ensureUserGroupExists(userGroupId);
        User user = userRepository.findById(request.userId())
                .filter(u -> "N".equals(u.getDeletedYn()))
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다: " + request.userId()));
        if (userGroupUserRepository.existsByUserGroupIdAndUserId(userGroupId, request.userId())) {
            throw new BusinessException(UserModuleErrorCode.USER_GROUP_USER_MAPPING_DUPLICATED,
                    "이미 이 그룹에 속한 사용자입니다: " + request.userId());
        }
        userGroupUserRepository.saveAndFlush(UserGroupUser.create(userGroupId, request.userId()));
        return UserDto.Response.from(user, effectiveRoleService.resolveRoles(user.getId()));
    }

    public List<UserDto.Response> getUsers(String userGroupId) {
        return userGroupUserRepository.findAllByUserGroupId(userGroupId).stream()
                .map(mapping -> userRepository.findById(mapping.getUserId()))
                .flatMap(java.util.Optional::stream)
                .map(user -> UserDto.Response.from(user, effectiveRoleService.resolveRoles(user.getId())))
                .toList();
    }

    /**
     * 프론트엔드는 목록 조회(getUsers)에서 User 정보를 받는데, 그 응답엔 매핑 자체의 PK가
     * 담겨있지 않다(User.id만 있음) — 그래서 매핑의 내부 PK가 아니라 userId로 직접 지운다.
     */
    @Transactional
    public void removeUser(String userGroupId, String userId) {
        UserGroupUser mapping = userGroupUserRepository.findByUserGroupIdAndUserId(userGroupId, userId)
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_GROUP_USER_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + userId));
        userGroupUserRepository.delete(mapping);
    }

    private void ensureUserGroupExists(String userGroupId) {
        userGroupRepository.findByIdAndDeletedYn(userGroupId, "N")
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_GROUP_NOT_FOUND,
                        "존재하지 않는 사용자그룹입니다: " + userGroupId));
    }
}
