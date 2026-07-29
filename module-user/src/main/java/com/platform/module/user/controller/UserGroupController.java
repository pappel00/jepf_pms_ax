package com.platform.module.user.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.user.dto.UserGroupDto;
import com.platform.module.user.service.UserGroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자그룹 관리 API. 사진 속 BackOffice "사용자그룹 관리" 화면 대응.
 * Role/User 컨트롤러와 동일하게, 다른 사용자의 권한 배정과 직결되는 민감한 정보라 클래스 전체를
 * ADMIN 권한으로 제한한다.
 */
@RestController
@RequestMapping("/api/v1/user-groups")
@Tag(name = "UserGroup", description = "사용자그룹 관리 (테넌트 자동 격리, ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class UserGroupController {

    private final UserGroupService userGroupService;

    public UserGroupController(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserGroupDto.Response> create(@Valid @RequestBody UserGroupDto.CreateRequest request) {
        return ApiResponse.success("201", "사용자그룹이 등록되었습니다.", userGroupService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserGroupDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(userGroupService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<UserGroupDto.Response>> list(Pageable pageable) {
        var result = userGroupService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    public ApiResponse<UserGroupDto.Response> update(@PathVariable String id,
                                                        @Valid @RequestBody UserGroupDto.UpdateRequest request) {
        return ApiResponse.success(userGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        userGroupService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
