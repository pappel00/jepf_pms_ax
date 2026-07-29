package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.MenuDto;
import com.platform.module.portal.service.MenuService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menus")
@Tag(name = "Menu", description = "메뉴 관리 (트리 구조, 테넌트 자동 격리)")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MenuDto.Response> create(@Valid @RequestBody MenuDto.CreateRequest request) {
        return ApiResponse.success("201", "메뉴가 등록되었습니다.", menuService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(menuService.getById(id));
    }

    /**
     * 계층 구조(트리) 그대로 조회. 프론트가 매번 parentMenuId로 직접 조립할 필요 없이
     * children을 그대로 재귀 렌더링하면 된다.
     * 예) GET /api/v1/menus/tree?applicationId=APP_xxx
     */
    @GetMapping("/tree")
    public ApiResponse<List<MenuDto.TreeNode>> tree(@RequestParam String applicationId) {
        return ApiResponse.success(menuService.getTree(applicationId));
    }

    @GetMapping
    public ApiResponse<List<MenuDto.Response>> list(
            @RequestParam(required = false) String applicationId, Pageable pageable) {
        var result = applicationId != null
                ? menuService.getListByApplication(applicationId, pageable)
                : menuService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MenuDto.Response> update(@PathVariable String id,
                                                  @Valid @RequestBody MenuDto.UpdateRequest request) {
        return ApiResponse.success(menuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        menuService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
