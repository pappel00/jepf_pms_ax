package com.platform.module.terms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.terms.dto.TermsGroupDto;
import com.platform.module.terms.service.TermsGroupService;
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
 * 이용약관(약관기본) 관리 API. 사진 속 "Data Management > 이용약관 관리" 화면 대응.
 * 조회는 로그인만 되어 있으면 가능, 생성/수정/삭제는 ADMIN 권한이 필요하다.
 *
 * 이용약관은 포탈별로 관리된다 — 생성 시 portalId가 필수이고, 목록 조회 시 portalId 쿼리
 * 파라미터로 특정 포탈의 약관만 걸러볼 수 있다(TermsGroup 클래스 주석 참고).
 */
@RestController
@RequestMapping("/api/v1/terms-groups")
@Tag(name = "TermsGroup", description = "이용약관 관리 (테넌트 자동 격리, 포탈별 관리)")
public class TermsGroupController {

    private final TermsGroupService termsGroupService;

    public TermsGroupController(TermsGroupService termsGroupService) {
        this.termsGroupService = termsGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TermsGroupDto.Response> create(@Valid @RequestBody TermsGroupDto.CreateRequest request) {
        return ApiResponse.success("201", "약관이 등록되었습니다.", termsGroupService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TermsGroupDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(termsGroupService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<TermsGroupDto.Response>> list(
            @RequestParam(required = false) String portalId, Pageable pageable) {
        var result = termsGroupService.getList(portalId, pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TermsGroupDto.Response> update(@PathVariable String id,
                                                         @Valid @RequestBody TermsGroupDto.UpdateRequest request) {
        return ApiResponse.success(termsGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        termsGroupService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
