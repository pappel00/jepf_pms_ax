package com.platform.module.user.service;

import com.platform.module.user.domain.UserGroupRole;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.repository.UserGroupRoleRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자의 최종 권한(effective roles)을 Role -> UserGroup -> User 3단계를 따라가 계산한다.
 * 1) 이 사용자가 속한 모든 UserGroup을 찾고
 * 2) 그 그룹들에 매핑된 모든 roleCode를 모아 중복 제거한 뒤 반환한다.
 *
 * AuthService(로그인/refresh 시 JWT roles claim 계산)와 UserService(조회 응답의 roles 필드)가
 * 이 서비스를 쓴다.
 */
@Service
@Transactional(readOnly = true)
public class EffectiveRoleService {

    private final UserGroupUserRepository userGroupUserRepository;
    private final UserGroupRoleRepository userGroupRoleRepository;

    public EffectiveRoleService(UserGroupUserRepository userGroupUserRepository,
                                 UserGroupRoleRepository userGroupRoleRepository) {
        this.userGroupUserRepository = userGroupUserRepository;
        this.userGroupRoleRepository = userGroupRoleRepository;
    }

    public List<String> resolveRoles(String userId) {
        List<String> groupIds = userGroupUserRepository.findAllByUserId(userId).stream()
                .map(UserGroupUser::getUserGroupId)
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        Set<String> roleCodes = userGroupRoleRepository.findAllByUserGroupIdIn(groupIds).stream()
                .map(UserGroupRole::getRoleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(roleCodes);
    }
}
