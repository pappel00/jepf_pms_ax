package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.ApplicationRoleDto;
import com.platform.module.portal.service.ApplicationRoleService;
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
 * 어플리케이션 관리 화면의 "권한" 부여 대응 — 어플리케이션 1 : Role N. 이 매핑 자체가
 * "누가 이 어플리케이션에 접근 가능한지"를 다루는 민감한 정보라, 조회를 포함해 전체 ADMIN
 * 권한으로 제한한다(PageRoleMappingController와 동일한 클래스 레벨 @PreAuthorize 패턴).
 *
 * 이 매핑은 ApplicationController.checkAccess()의 실제 인가 로직에 연결되어 있다.
 */
@RestController
@RequestMapping("/api/v1/applications/{applicationId}/roles")
@Tag(name = "ApplicationRoleMapping", description = "어플리케이션-권한(Role) 매핑 (ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class ApplicationRoleMappingController {

    private final ApplicationRoleService applicationRoleService;

    public ApplicationRoleMappingController(ApplicationRoleService applicationRoleService) {
        this.applicationRoleService = applicationRoleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationRoleDto.Response> create(
            @PathVariable String applicationId, @Valid @RequestBody ApplicationRoleDto.CreateRequest request) {
        return ApiResponse.success("201", "역할이 어플리케이션에 매핑되었습니다.",
                applicationRoleService.create(applicationId, request));
    }

    @GetMapping
    public ApiResponse<List<ApplicationRoleDto.Response>> list(@PathVariable String applicationId) {
        return ApiResponse.success(applicationRoleService.getList(applicationId));
    }

    @DeleteMapping("/{mappingId}")
    public ApiResponse<Void> delete(@PathVariable String applicationId, @PathVariable String mappingId) {
        applicationRoleService.delete(applicationId, mappingId);
        return ApiResponse.success("200", "매핑이 삭제되었습니다.", null);
    }
}
