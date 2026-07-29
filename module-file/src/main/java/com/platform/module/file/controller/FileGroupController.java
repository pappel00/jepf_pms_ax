package com.platform.module.file.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.file.dto.FileGroupDto;
import com.platform.module.file.service.FileGroupService;
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
 * 첨부파일 그룹(첨부그룹) 관리 API. 사진 속 "Data Management > 첨부파일 그룹 관리" 화면 대응.
 * 조회는 로그인만 되어 있으면 가능, 생성/수정/삭제는 ADMIN 권한이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/file-groups")
@Tag(name = "FileGroup", description = "첨부파일 그룹 관리 (테넌트 자동 격리)")
public class FileGroupController {

    private final FileGroupService fileGroupService;

    public FileGroupController(FileGroupService fileGroupService) {
        this.fileGroupService = fileGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FileGroupDto.Response> create(@Valid @RequestBody FileGroupDto.CreateRequest request) {
        return ApiResponse.success("201", "첨부그룹이 등록되었습니다.", fileGroupService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<FileGroupDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(fileGroupService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<FileGroupDto.Response>> list(Pageable pageable) {
        var result = fileGroupService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FileGroupDto.Response> update(@PathVariable String id,
                                                        @Valid @RequestBody FileGroupDto.UpdateRequest request) {
        return ApiResponse.success(fileGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        fileGroupService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
