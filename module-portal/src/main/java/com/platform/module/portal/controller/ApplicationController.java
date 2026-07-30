package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.ApplicationDto;
import com.platform.module.portal.service.ApplicationRoleService;
import com.platform.module.portal.service.ApplicationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Application", description = "어플리케이션 관리 (테넌트 자동 격리)")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationRoleService applicationRoleService;

    public ApplicationController(ApplicationService applicationService, ApplicationRoleService applicationRoleService) {
        this.applicationService = applicationService;
        this.applicationRoleService = applicationRoleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ApplicationDto.Response> create(@Valid @RequestBody ApplicationDto.CreateRequest request) {
        return ApiResponse.success("201", "어플리케이션이 등록되었습니다.", applicationService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(applicationService.getById(id));
    }

    /**
     * accessibleOnly=true면 관리자용 페이지네이션 목록 대신, 현재 로그인한 사용자가 실제로
     * 접근 가능한 어플리케이션만 sortOrder 순으로 내려준다(운영 화면 셸의 어플리케이션 선택
     * UI 전용 — ApplicationService.getAccessibleList() 참고). 이 경로는 페이지네이션
     * 메타데이터가 의미 없어서 pagination 없이 순수 목록만 응답한다.
     */
    @GetMapping
    public ApiResponse<List<ApplicationDto.Response>> list(
            @RequestParam(required = false) String portalId,
            @RequestParam(required = false, defaultValue = "false") boolean accessibleOnly,
            Authentication authentication,
            Pageable pageable) {
        if (accessibleOnly) {
            List<String> userAuthorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            return ApiResponse.success(applicationService.getAccessibleList(portalId, userAuthorities));
        }
        var result = portalId != null
                ? applicationService.getListByPortal(portalId, pageable)
                : applicationService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    /**
     * 현재 로그인한 사용자가 이 어플리케이션에 접근 가능한지 ApplicationRole 매핑 기준으로
     * 판단한다. 매핑이 하나도 없는 어플리케이션은 로그인한 사용자 누구나 접근 가능하다고 본다
     * (PageController.checkAccess()와 동일한 패턴).
     */
    @GetMapping("/{id}/access-check")
    public ApiResponse<Boolean> checkAccess(@PathVariable String id, Authentication authentication) {
        List<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return ApiResponse.success(applicationRoleService.isAccessible(id, userAuthorities));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ApplicationDto.Response> update(@PathVariable String id,
                                                         @Valid @RequestBody ApplicationDto.UpdateRequest request) {
        return ApiResponse.success(applicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        applicationService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable String id) {
        applicationService.restore(id);
        return ApiResponse.success("200", "복구되었습니다.", null);
    }
}
