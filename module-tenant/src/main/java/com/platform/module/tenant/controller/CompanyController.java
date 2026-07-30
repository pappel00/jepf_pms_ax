package com.platform.module.tenant.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.tenant.dto.CompanyDto;
import com.platform.module.tenant.service.CompanyService;
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
 * 회사(테넌트) 마스터 관리 API.
 * 회사 자체는 테넌트 경계를 정의하는 주체이므로, 생성/수정/삭제는 SYSTEM_ADMIN 권한으로만 호출 가능하다.
 * 조회(GET)는 로그인만 되어 있으면 누구나 가능하다.
 */
@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Company", description = "회사(테넌트) 마스터 관리")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ApiResponse<CompanyDto.Response> create(@Valid @RequestBody CompanyDto.CreateRequest request) {
        return ApiResponse.success("201", "회사가 등록되었습니다.", companyService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(companyService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<CompanyDto.Response>> list(Pageable pageable) {
        var result = companyService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ApiResponse<CompanyDto.Response> update(@PathVariable String id,
                                                     @Valid @RequestBody CompanyDto.UpdateRequest request) {
        return ApiResponse.success(companyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        companyService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ApiResponse<Void> restore(@PathVariable String id) {
        companyService.restore(id);
        return ApiResponse.success("200", "복구되었습니다.", null);
    }
}
