package com.platform.module.user.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.user.dto.UserGroupRoleDto;
import com.platform.module.user.service.UserGroupRoleService;
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
 * 사진 속 "사용자그룹 관리"/"Role 관리" 화면의 "권한"/"사용자그룹" 탭 대응.
 * 이 그룹에 Role을 부여/제거한다 — 이 그룹에 속한 모든 사용자가 이 Role을 갖게 된다.
 */
@RestController
@RequestMapping("/api/v1/user-groups/{userGroupId}/roles")
@Tag(name = "UserGroupRoleMapping", description = "사용자그룹-역할 매핑 (ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class UserGroupRoleController {

    private final UserGroupRoleService userGroupRoleService;

    public UserGroupRoleController(UserGroupRoleService userGroupRoleService) {
        this.userGroupRoleService = userGroupRoleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserGroupRoleDto.Response> create(
            @PathVariable String userGroupId, @Valid @RequestBody UserGroupRoleDto.CreateRequest request) {
        return ApiResponse.success("201", "역할이 그룹에 매핑되었습니다.", userGroupRoleService.create(userGroupId, request));
    }

    @GetMapping
    public ApiResponse<List<UserGroupRoleDto.Response>> list(@PathVariable String userGroupId) {
        return ApiResponse.success(userGroupRoleService.getList(userGroupId));
    }

    @DeleteMapping("/{mappingId}")
    public ApiResponse<Void> delete(@PathVariable String userGroupId, @PathVariable String mappingId) {
        userGroupRoleService.delete(userGroupId, mappingId);
        return ApiResponse.success("200", "매핑이 삭제되었습니다.", null);
    }
}
