package com.platform.module.user.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.user.domain.User;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.dto.UserDto;
import com.platform.module.user.exception.UserModuleErrorCode;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이 서비스는 이미 인증이 완료된 요청(JwtAuthenticationFilter가 TenantContext를 채운 이후)에서만
 * 호출되므로, AuthService와 달리 일반적인 @Transactional 을 그대로 사용해도 안전하다.
 *
 * ⚠️ User는 더 이상 역할을 직접 갖지 않는다(User.java 클래스 주석 참고). 예전에 여기 있던
 * updateRoles()/validateRoleCodes()는 UserGroupRoleService로 옮겨갔고, 역할 조회는
 * EffectiveRoleService가 담당한다.
 *
 * create()는 관리자가 사용자 관리 화면에서 직접 사용자를 만드는 용도다. 회원가입
 * (AuthService.register)과 마찬가지로 defaultYn="Y"인 UserGroup이 있으면 자동으로 배정한다 —
 * 두 경로 다 "새로 생긴 사용자는 최소한 기본권한 그룹에 들어가 있어야 한다"는 동작을 그대로
 * 유지해야 하기 때문이다.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupUserRepository userGroupUserRepository;
    private final EffectiveRoleService effectiveRoleService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                        UserGroupRepository userGroupRepository,
                        UserGroupUserRepository userGroupUserRepository,
                        EffectiveRoleService effectiveRoleService,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupUserRepository = userGroupUserRepository;
        this.effectiveRoleService = effectiveRoleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto.Response create(UserDto.CreateRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(UserModuleErrorCode.LOGIN_ID_DUPLICATED,
                    "이미 존재하는 로그인 아이디입니다: " + request.loginId());
        }
        User user = User.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.email(),
                request.userName(),
                request.departmentId());
        User saved = userRepository.saveAndFlush(user);

        userGroupRepository.findFirstByDefaultYnAndDeletedYn("Y", "N")
                .ifPresent(defaultGroup -> assignToDefaultGroup(saved.getId(), defaultGroup));

        return UserDto.Response.from(saved, effectiveRoleService.resolveRoles(saved.getId()));
    }

    public UserDto.Response getById(String id) {
        User user = findActiveOrThrow(id);
        return UserDto.Response.from(user, effectiveRoleService.resolveRoles(user.getId()));
    }

    public PagedResult<UserDto.Response> getList(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return new PagedResult<>(
                page.map(user -> UserDto.Response.from(user, effectiveRoleService.resolveRoles(user.getId()))).getContent(),
                PageInfo.of(page));
    }

    @Transactional
    public UserDto.Response update(String id, UserDto.UpdateRequest request) {
        User user = findActiveOrThrow(id);
        user.updateProfile(request.email(), request.userName());
        user.assignDepartment(request.departmentId());
        if ("Y".equals(request.useYn())) {
            user.activate();
        } else {
            user.deactivate();
        }
        return UserDto.Response.from(user, effectiveRoleService.resolveRoles(user.getId()));
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private void assignToDefaultGroup(String userId, UserGroup group) {
        if (!userGroupUserRepository.existsByUserGroupIdAndUserId(group.getId(), userId)) {
            userGroupUserRepository.saveAndFlush(UserGroupUser.create(group.getId(), userId));
        }
    }

    private User findActiveOrThrow(String id) {
        return userRepository.findById(id)
                .filter(user -> "N".equals(user.getDeletedYn()))
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다: " + id));
    }
}
