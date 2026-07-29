package com.platform.module.user.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.user.dto.RoleUserGroupDto;
import com.platform.module.user.dto.UserGroupDto;
import com.platform.module.user.dto.UserGroupRoleDto;
import com.platform.module.user.service.RoleUserGroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role 관점에서 "이 역할이 매핑된 사용자그룹" 조회/추가/제거.
 * UserGroupRoleController(/api/v1/user-groups/{userGroupId}/roles)와 저장소를 공유하는
 * 반대 방향 API다 — 어느 쪽으로 추가하든 결과는 같은 UserGroupRole 매핑이다.
 */
@RestController
@RequestMapping("/api/v1/roles/{roleId}/user-groups")
@Tag(name = "RoleUserGroupMapping", description = "역할-사용자그룹 매핑 (Role 관점, ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class RoleUserGroupController {

    private final RoleUserGroupService roleUserGroupService;

    public RoleUserGroupController(RoleUserGroupService roleUserGroupService) {
        this.roleUserGroupService = roleUserGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserGroupRoleDto.Response> add(
            @PathVariable String roleId, @Valid @RequestBody RoleUserGroupDto.CreateRequest request) {
        return ApiResponse.success("201", "사용자그룹에 역할이 매핑되었습니다.", roleUserGroupService.addUserGroup(roleId, request));
    }

    @GetMapping
    public ApiResponse<List<UserGroupDto.Response>> list(@PathVariable String roleId) {
        return ApiResponse.success(roleUserGroupService.getUserGroups(roleId));
    }

    @DeleteMapping("/{mappingId}")
    public ApiResponse<Void> remove(@PathVariable String roleId, @PathVariable String mappingId) {
        roleUserGroupService.removeUserGroup(roleId, mappingId);
        return ApiResponse.success("200", "매핑이 삭제되었습니다.", null);
    }
}
