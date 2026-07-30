package com.platform.module.form.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.form.dto.FormGroupDto;
import com.platform.module.form.service.FormGroupService;
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
 * 표준양식(양식기본) 관리 API. 사진 속 "Data Management > 표준양식 관리" 화면 대응.
 * 조회는 로그인만 되어 있으면 가능, 생성/수정/삭제는 ADMIN 권한이 필요하다.
 *
 * 이용약관/개인정보처리방침과 동일하게 포탈별로 관리된다 — 생성 시 portalId가 필수이고,
 * 목록 조회 시 portalId 쿼리 파라미터로 특정 포탈의 양식만 걸러볼 수 있다.
 */
@RestController
@RequestMapping("/api/v1/form-groups")
@Tag(name = "FormGroup", description = "표준양식 관리 (테넌트 자동 격리, 포탈별 관리)")
public class FormGroupController {

    private final FormGroupService formGroupService;

    public FormGroupController(FormGroupService formGroupService) {
        this.formGroupService = formGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FormGroupDto.Response> create(@Valid @RequestBody FormGroupDto.CreateRequest request) {
        return ApiResponse.success("201", "표준양식이 등록되었습니다.", formGroupService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<FormGroupDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(formGroupService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<FormGroupDto.Response>> list(
            @RequestParam(required = false) String portalId, Pageable pageable) {
        var result = formGroupService.getList(portalId, pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FormGroupDto.Response> update(
            @PathVariable String id, @Valid @RequestBody FormGroupDto.UpdateRequest request) {
        return ApiResponse.success(formGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        formGroupService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable String id) {
        formGroupService.restore(id);
        return ApiResponse.success("200", "복구되었습니다.", null);
    }
}
