package com.platform.module.privacy.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.privacy.dto.PrivacyPolicyVersionDto;
import com.platform.module.privacy.service.PrivacyPolicyVersionService;
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
 * 개인정보처리방침 버전(개정 이력) 관리 API. PrivacyPolicyGroup의 하위 리소스다 — 사진 속
 * "개인정보처리방침 관리" 화면에서 방침을 선택하면 하단에 보이는 버전 이력 목록에 대응한다.
 */
@RestController
@RequestMapping("/api/v1/privacy-policy-groups/{groupId}/versions")
@Tag(name = "PrivacyPolicyVersion", description = "개인정보처리방침 버전 관리 (테넌트 자동 격리)")
public class PrivacyPolicyVersionController {

    private final PrivacyPolicyVersionService privacyPolicyVersionService;

    public PrivacyPolicyVersionController(PrivacyPolicyVersionService privacyPolicyVersionService) {
        this.privacyPolicyVersionService = privacyPolicyVersionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PrivacyPolicyVersionDto.Response> create(
            @PathVariable String groupId, @Valid @RequestBody PrivacyPolicyVersionDto.CreateRequest request) {
        return ApiResponse.success("201", "방침 버전이 등록되었습니다.", privacyPolicyVersionService.create(groupId, request));
    }

    @GetMapping
    public ApiResponse<List<PrivacyPolicyVersionDto.Response>> list(@PathVariable String groupId) {
        return ApiResponse.success(privacyPolicyVersionService.getList(groupId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PrivacyPolicyVersionDto.Response> update(
            @PathVariable String groupId, @PathVariable String id,
            @Valid @RequestBody PrivacyPolicyVersionDto.UpdateRequest request) {
        return ApiResponse.success(privacyPolicyVersionService.update(groupId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String groupId, @PathVariable String id) {
        privacyPolicyVersionService.delete(groupId, id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
