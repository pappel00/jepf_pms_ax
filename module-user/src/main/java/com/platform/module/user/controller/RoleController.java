package com.platform.module.user.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.user.dto.RoleDto;
import com.platform.module.user.service.RoleService;
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
 * 역할(Role) 마스터 관리 API. 요청자의 테넌트로 자동 범위 제한된다.
 * 역할을 정의하는 것 자체가 관리자 권한이므로 클래스 전체를 ADMIN 권한으로 제한한다.
 */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Role", description = "역할(Role) 마스터 관리 (테넌트 자동 격리, ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoleDto.Response> create(@Valid @RequestBody RoleDto.CreateRequest request) {
        return ApiResponse.success("201", "역할이 등록되었습니다.", roleService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<RoleDto.Response>> list(Pageable pageable) {
        var result = roleService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleDto.Response> update(@PathVariable String id,
                                                  @Valid @RequestBody RoleDto.UpdateRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable String id) {
        roleService.restore(id);
        return ApiResponse.success("200", "복구되었습니다.", null);
    }
}
