package com.platform.module.terms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.terms.dto.TermsVersionDto;
import com.platform.module.terms.service.TermsVersionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
 * 이용약관 버전(개정 이력) 관리 API. TermsGroup의 하위 리소스다 — 사진 속 "이용약관 관리"
 * 화면에서 약관을 선택하면 하단에 보이는 버전 이력 목록에 대응한다.
 */
@RestController
@RequestMapping("/api/v1/terms-groups/{groupId}/versions")
@Tag(name = "TermsVersion", description = "이용약관 버전 관리 (테넌트 자동 격리)")
public class TermsVersionController {

    private final TermsVersionService termsVersionService;

    public TermsVersionController(TermsVersionService termsVersionService) {
        this.termsVersionService = termsVersionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TermsVersionDto.Response> create(
            @PathVariable String groupId, @Valid @RequestBody TermsVersionDto.CreateRequest request) {
        return ApiResponse.success("201", "약관 버전이 등록되었습니다.", termsVersionService.create(groupId, request));
    }

    @GetMapping
    public ApiResponse<List<TermsVersionDto.Response>> list(@PathVariable String groupId) {
        return ApiResponse.success(termsVersionService.getList(groupId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TermsVersionDto.Response> update(
            @PathVariable String groupId, @PathVariable String id,
            @Valid @RequestBody TermsVersionDto.UpdateRequest request) {
        return ApiResponse.success(termsVersionService.update(groupId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String groupId, @PathVariable String id) {
        termsVersionService.delete(groupId, id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
