package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.PageDto;
import com.platform.module.portal.service.PageRoleService;
import com.platform.module.portal.service.PageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pages")
@Tag(name = "Page", description = "페이지 관리 (테넌트 자동 격리)")
public class PageController {

    private final PageService pageService;
    private final PageRoleService pageRoleService;

    public PageController(PageService pageService, PageRoleService pageRoleService) {
        this.pageService = pageService;
        this.pageRoleService = pageRoleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageDto.Response> create(@Valid @RequestBody PageDto.CreateRequest request) {
        return ApiResponse.success("201", "페이지가 등록되었습니다.", pageService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PageDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(pageService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<PageDto.Response>> list(Pageable pageable) {
        var result = pageService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    /**
     * 현재 로그인한 사용자가 이 페이지에 접근 가능한지 PageRole 매핑 기준으로 판단한다.
     * 매핑이 하나도 없는 페이지는 로그인한 사용자 누구나 접근 가능하다고 본다.
     */
    @GetMapping("/{id}/access-check")
    public ApiResponse<Boolean> checkAccess(@PathVariable String id, Authentication authentication) {
        List<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return ApiResponse.success(pageRoleService.isAccessible(id, userAuthorities));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageDto.Response> update(@PathVariable String id,
                                                  @Valid @RequestBody PageDto.UpdateRequest request) {
        return ApiResponse.success(pageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        pageService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable String id) {
        pageService.restore(id);
        return ApiResponse.success("200", "복구되었습니다.", null);
    }
}
