package com.platform.module.code.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.code.dto.CodeDetailDto;
import com.platform.module.code.service.CodeDetailService;
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
 * 코드상세 관리 API. CodeGroup의 하위 리소스다 — 사진 속 "코드 관리" 화면에서 코드그룹을
 * 선택하면 하단에 보이는 상세 목록에 대응한다.
 */
@RestController
@RequestMapping("/api/v1/code-groups/{groupId}/details")
@Tag(name = "CodeDetail", description = "코드상세 관리 (테넌트 자동 격리)")
public class CodeDetailController {

    private final CodeDetailService codeDetailService;

    public CodeDetailController(CodeDetailService codeDetailService) {
        this.codeDetailService = codeDetailService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CodeDetailDto.Response> create(
            @PathVariable String groupId, @Valid @RequestBody CodeDetailDto.CreateRequest request) {
        return ApiResponse.success("201", "코드가 등록되었습니다.", codeDetailService.create(groupId, request));
    }

    @GetMapping
    public ApiResponse<List<CodeDetailDto.Response>> list(@PathVariable String groupId) {
        return ApiResponse.success(codeDetailService.getList(groupId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CodeDetailDto.Response> update(
            @PathVariable String groupId, @PathVariable String id,
            @Valid @RequestBody CodeDetailDto.UpdateRequest request) {
        return ApiResponse.success(codeDetailService.update(groupId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String groupId, @PathVariable String id) {
        codeDetailService.delete(groupId, id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
