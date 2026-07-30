package com.platform.module.tenant.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.tenant.dto.DepartmentDto;
import com.platform.module.tenant.service.DepartmentService;
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
 * 부서 관리 API. 모든 응답은 현재 로그인한 사용자의 테넌트로 자동 범위 제한된다
 * (Hibernate @TenantId, Company와 달리 별도 tenantId 파라미터가 필요 없다).
 * 생성/수정/삭제는 ADMIN 권한이 필요하고, 조회(GET)는 로그인만 되어 있으면 가능하다.
 */
@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Department", description = "부서 관리 (테넌트 자동 격리)")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentDto.Response> create(@Valid @RequestBody DepartmentDto.CreateRequest request) {
        return ApiResponse.success("201", "부서가 등록되었습니다.", departmentService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<DepartmentDto.Response>> list(Pageable pageable) {
        var result = departmentService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentDto.Response> update(@PathVariable String id,
                                                        @Valid @RequestBody DepartmentDto.UpdateRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        departmentService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable String id) {
        departmentService.restore(id);
        return ApiResponse.success("200", "복구되었습니다.", null);
    }
}
