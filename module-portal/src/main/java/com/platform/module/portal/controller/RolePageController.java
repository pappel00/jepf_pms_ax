package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.PageDto;
import com.platform.module.portal.dto.PageRoleDto;
import com.platform.module.portal.dto.RolePageDto;
import com.platform.module.portal.service.RolePageService;
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
 * roleCode 관점에서 "이 역할코드가 매핑된 페이지" 조회/추가/제거.
 * PageRoleMappingController(/api/v1/pages/{pageId}/roles, Page 관점)와 저장소를 공유하는
 * 반대 방향 API다.
 *
 * ⚠️ 경로가 /api/v1/roles/{roleId}/pages 가 아니라 /api/v1/pages/by-role/{roleCode} 인 이유:
 * module-portal은 module-user(Role)에 의존하지 않아서 Role의 PK를 조회할 수 없다. 그래서
 * roleId(PK)가 아니라 roleCode 문자열(User.roles/GrantedAuthority와 같은 값)을 그대로 받는다.
 */
@RestController
@RequestMapping("/api/v1/pages/by-role/{roleCode}")
@Tag(name = "RolePageMapping", description = "역할-페이지 매핑 (roleCode 기준, ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class RolePageController {

    private final RolePageService rolePageService;

    public RolePageController(RolePageService rolePageService) {
        this.rolePageService = rolePageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PageRoleDto.Response> add(
            @PathVariable String roleCode, @Valid @RequestBody RolePageDto.CreateRequest request) {
        return ApiResponse.success("201", "페이지에 역할이 매핑되었습니다.", rolePageService.addPage(roleCode, request));
    }

    @GetMapping
    public ApiResponse<List<PageDto.Response>> list(@PathVariable String roleCode) {
        return ApiResponse.success(rolePageService.getPages(roleCode));
    }

    @DeleteMapping("/{mappingId}")
    public ApiResponse<Void> remove(@PathVariable String roleCode, @PathVariable String mappingId) {
        rolePageService.removePage(roleCode, mappingId);
        return ApiResponse.success("200", "매핑이 삭제되었습니다.", null);
    }
}
