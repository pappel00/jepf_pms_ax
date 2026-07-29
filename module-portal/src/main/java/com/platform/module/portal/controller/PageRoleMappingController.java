package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.PageRoleDto;
import com.platform.module.portal.service.PageRoleService;
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
 * 사진 속 페이지 관리 화면의 "권한" 탭 대응. 이 매핑 자체가 "누가 이 페이지에 접근 가능한지"를
 * 다루는 민감한 정보라, 조회를 포함해 전체 ADMIN 권한으로 제한한다 (DataService/Role 컨트롤러와
 * 동일한 클래스 레벨 @PreAuthorize 패턴).
 *
 * 이 매핑은 실제 인가 로직에 연결되어 있다:
 *  - PageController.checkAccess() — 특정 사용자가 특정 페이지에 접근 가능한지 직접 조회
 *  - MenuController/MenuService.getTree() — 접근 불가능한 페이지가 연결된 메뉴는 트리에서 자동으로 제외
 */
@RestController
@RequestMapping("/api/v1/pages/{pageId}/roles")
@Tag(name = "PageRoleMapping", description = "페이지-권한(Role) 매핑 (ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class PageRoleMappingController {

    private final PageRoleService pageRoleService;

    public PageRoleMappingController(PageRoleService pageRoleService) {
        this.pageRoleService = pageRoleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PageRoleDto.Response> create(
            @PathVariable String pageId, @Valid @RequestBody PageRoleDto.CreateRequest request) {
        return ApiResponse.success("201", "역할이 페이지에 매핑되었습니다.", pageRoleService.create(pageId, request));
    }

    @GetMapping
    public ApiResponse<List<PageRoleDto.Response>> list(@PathVariable String pageId) {
        return ApiResponse.success(pageRoleService.getList(pageId));
    }

    @DeleteMapping("/{mappingId}")
    public ApiResponse<Void> delete(@PathVariable String pageId, @PathVariable String mappingId) {
        pageRoleService.delete(pageId, mappingId);
        return ApiResponse.success("200", "매핑이 삭제되었습니다.", null);
    }
}
