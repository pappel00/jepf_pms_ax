package com.platform.module.user.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.user.domain.Role;
import com.platform.module.user.dto.RoleDto;
import com.platform.module.user.exception.UserModuleErrorCode;
import com.platform.module.user.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이 서비스는 인증된 요청(JwtAuthenticationFilter가 TenantContext를 채운 이후)에서만 호출되므로
 * AuthService와 달리 일반적인 @Transactional을 그대로 사용해도 안전하다.
 */
@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public RoleDto.Response create(RoleDto.CreateRequest request) {
        if (roleRepository.existsByRoleCode(request.roleCode())) {
            throw new BusinessException(UserModuleErrorCode.ROLE_CODE_DUPLICATED,
                    "이미 존재하는 역할 코드입니다: " + request.roleCode());
        }
        Role role = Role.create(request.roleCode(), request.roleName(), request.roleAlias(),
                request.category(), request.description());
        return RoleDto.Response.from(roleRepository.saveAndFlush(role));
    }

    public RoleDto.Response getById(String id) {
        return RoleDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<RoleDto.Response> getList(Pageable pageable) {
        Page<Role> page = roleRepository.findAll(pageable);
        return new PagedResult<>(page.map(RoleDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public RoleDto.Response update(String id, RoleDto.UpdateRequest request) {
        Role role = findActiveOrThrow(id);
        role.update(request.roleName(), request.roleAlias(), request.category(), request.description());
        if ("Y".equals(request.useYn())) {
            role.activate();
        } else {
            role.deactivate();
        }
        return RoleDto.Response.from(role);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private Role findActiveOrThrow(String id) {
        return roleRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.ROLE_NOT_FOUND,
                        "존재하지 않는 역할입니다: " + id));
    }
}
