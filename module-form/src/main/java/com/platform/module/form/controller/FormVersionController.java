package com.platform.module.form.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.form.dto.FormVersionDto;
import com.platform.module.form.service.FormVersionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
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
 * 표준양식 버전(개정 이력) 관리 API. FormGroup의 하위 리소스다 — 사진 속 "표준양식 관리"
 * 화면에서 양식을 선택하면 하단에 보이는 버전 이력 목록에 대응한다.
 *
 * 텍스트가 아니라 실제 엑셀/워드 파일로 관리한다 — create()는 filePath를 직접 입력하는
 * 메타데이터 전용 등록(실제 파일 없이)이고, upload()는 실제 파일을 받아서 디스크에 저장까지
 * 하는 진짜 업로드다. module-file과 동일한 방식(FileStorageProperties.base-path)이다.
 */
@RestController
@RequestMapping("/api/v1/form-groups/{groupId}/versions")
@Tag(name = "FormVersion", description = "표준양식 버전 관리 (테넌트 자동 격리)")
public class FormVersionController {

    private final FormVersionService formVersionService;

    public FormVersionController(FormVersionService formVersionService) {
        this.formVersionService = formVersionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FormVersionDto.Response> create(
            @PathVariable String groupId, @Valid @RequestBody FormVersionDto.CreateRequest request) {
        return ApiResponse.success("201", "양식 버전이 등록되었습니다.", formVersionService.create(groupId, request));
    }

    /** 실제 파일(엑셀/워드 등)을 업로드한다 (multipart/form-data). fileName/fileSize는 업로드된 파일에서 그대로 가져온다. */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FormVersionDto.Response> upload(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("versionNo") String versionNo,
            @RequestParam("effectiveDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam(value = "description", required = false) String description) {
        return ApiResponse.success("201", "파일이 업로드되었습니다.",
                formVersionService.upload(groupId, file, versionNo, effectiveDate, description));
    }

    /** 표준양식 버전의 실제 파일을 다운로드한다. 로그인만 되어 있으면 가능하다(조회 권한과 동일). */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String groupId, @PathVariable String id) throws IOException {
        FormVersionService.DownloadTarget target = formVersionService.getDownloadTarget(groupId, id);
        Resource resource = new InputStreamResource(Files.newInputStream(target.diskPath()));
        String encodedFileName = URLEncoder.encode(target.fileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @GetMapping
    public ApiResponse<List<FormVersionDto.Response>> list(@PathVariable String groupId) {
        return ApiResponse.success(formVersionService.getList(groupId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FormVersionDto.Response> update(
            @PathVariable String groupId, @PathVariable String id,
            @Valid @RequestBody FormVersionDto.UpdateRequest request) {
        return ApiResponse.success(formVersionService.update(groupId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String groupId, @PathVariable String id) {
        formVersionService.delete(groupId, id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable String groupId, @PathVariable String id) {
        formVersionService.restore(groupId, id);
        return ApiResponse.success("200", "복구되었습니다.", null);
    }
}
