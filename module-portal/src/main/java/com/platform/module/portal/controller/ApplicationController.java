package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.ApplicationDto;
import com.platform.module.portal.service.ApplicationService;
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
@RequestMapping("/api/v1/applications")
@Tag(name = "Application", description = "어플리케이션 관리 (테넌트 자동 격리)")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
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

    @GetMapping
    public ApiResponse<List<ApplicationDto.Response>> list(
            @RequestParam(required = false) String portalId, Pageable pageable) {
        var result = portalId != null
                ? applicationService.getListByPortal(portalId, pageable)
                : applicationService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
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
}
