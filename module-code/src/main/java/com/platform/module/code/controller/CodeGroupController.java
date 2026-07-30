package com.platform.module.code.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.code.dto.CodeGroupDto;
import com.platform.module.code.service.CodeGroupService;
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
 * 코드그룹(코드기본) 관리 API. 사진 속 "Data Management > 코드 관리" 화면 대응.
 * 조회는 로그인만 되어 있으면 가능(다른 모듈들이 코드값을 참조해서 표시해야 할 수 있음),
 * 생성/수정/삭제는 ADMIN 권한이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/code-groups")
@Tag(name = "CodeGroup", description = "코드그룹(코드기본) 관리 (테넌트 자동 격리)")
public class CodeGroupController {

    private final CodeGroupService codeGroupService;

    public CodeGroupController(CodeGroupService codeGroupService) {
        this.codeGroupService = codeGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CodeGroupDto.Response> create(@Valid @RequestBody CodeGroupDto.CreateRequest request) {
        return ApiResponse.success("201", "코드그룹이 등록되었습니다.", codeGroupService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CodeGroupDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(codeGroupService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<CodeGroupDto.Response>> list(Pageable pageable) {
        var result = codeGroupService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CodeGroupDto.Response> update(@PathVariable String id,
                                                        @Valid @RequestBody CodeGroupDto.UpdateRequest request) {
        return ApiResponse.success(codeGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        codeGroupService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable String id) {
        codeGroupService.restore(id);
        return ApiResponse.success("200", "복구되었습니다.", null);
    }
}
