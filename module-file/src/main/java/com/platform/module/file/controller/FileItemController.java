package com.platform.module.file.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.file.dto.FileItemDto;
import com.platform.module.file.service.FileItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부항목 관리 API. FileGroup의 하위 리소스다 — 사진 속 "첨부파일 그룹 관리" 화면에서
 * 첨부그룹을 선택하면 하단에 보이는 첨부항목 목록에 대응한다.
 *
 * create()는 filePath를 직접 입력하는 메타데이터 전용 등록(실제 파일 없이)이고,
 * upload()는 실제 파일을 받아서 디스크에 저장까지 하는 진짜 업로드다. 저장 위치는
 * application.yml의 framework.file.storage.base-path로 설정한다(FileStorageProperties 참고).
 */
@RestController
@RequestMapping("/api/v1/file-groups/{groupId}/items")
@Tag(name = "FileItem", description = "첨부항목 관리 (테넌트 자동 격리)")
public class FileItemController {

    private final FileItemService fileItemService;

    public FileItemController(FileItemService fileItemService) {
        this.fileItemService = fileItemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FileItemDto.Response> create(
            @PathVariable String groupId, @Valid @RequestBody FileItemDto.CreateRequest request) {
        return ApiResponse.success("201", "첨부항목이 등록되었습니다.", fileItemService.create(groupId, request));
    }

    /** 실제 파일을 업로드한다 (multipart/form-data). fileName/fileSize는 업로드된 파일에서 그대로 가져온다. */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FileItemDto.Response> upload(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(value = "description", required = false) String description) {
        return ApiResponse.success("201", "파일이 업로드되었습니다.",
                fileItemService.upload(groupId, file, sortOrder, description));
    }

    /** 첨부항목의 실제 파일을 다운로드한다. 로그인만 되어 있으면 가능하다(조회 권한과 동일). */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String groupId, @PathVariable String id) throws IOException {
        FileItemService.DownloadTarget target = fileItemService.getDownloadTarget(groupId, id);
        Resource resource = new InputStreamResource(Files.newInputStream(target.diskPath()));
        String encodedFileName = URLEncoder.encode(target.fileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @GetMapping
    public ApiResponse<List<FileItemDto.Response>> list(@PathVariable String groupId) {
        return ApiResponse.success(fileItemService.getList(groupId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FileItemDto.Response> update(
            @PathVariable String groupId, @PathVariable String id,
            @Valid @RequestBody FileItemDto.UpdateRequest request) {
        return ApiResponse.success(fileItemService.update(groupId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String groupId, @PathVariable String id) {
        fileItemService.delete(groupId, id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
