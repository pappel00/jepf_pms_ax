package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.ProjectDto;
import com.platform.module.pms.service.ProjectService;
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

/**
 * 프로젝트(사업) API. 조회·등록·수정·상태 변경은 로그인 사용자, 삭제는 ADMIN.
 * (프로젝트 멤버 기반 조회 범위·접근 권한은 멤버 단계에서 추가한다.)
 */
@RestController
@RequestMapping("/api/v1/pms/projects")
@Tag(name = "PmsProject", description = "PMS 프로젝트(사업) 관리 (테넌트 자동 격리)")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectDto.Response> create(@Valid @RequestBody ProjectDto.Request request) {
        return ApiResponse.success("201", "프로젝트가 등록되었습니다.", projectService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(projectService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<ProjectDto.Response>> list(@RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String customerId,
                                                       @RequestParam(required = false) String salesUserId,
                                                       @RequestParam(required = false) String keyword,
                                                       Pageable pageable) {
        var result = projectService.getList(status, customerId, salesUserId, keyword, pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectDto.Response> update(@PathVariable String id, @Valid @RequestBody ProjectDto.Request request) {
        return ApiResponse.success(projectService.update(id, request));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<ProjectDto.Response> changeStatus(@PathVariable String id,
                                                         @Valid @RequestBody ProjectDto.StatusRequest request) {
        return ApiResponse.success(projectService.changeStatus(id, request));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<ProjectDto.HistoryResponse>> history(@PathVariable String id) {
        return ApiResponse.success(projectService.getHistory(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        projectService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}