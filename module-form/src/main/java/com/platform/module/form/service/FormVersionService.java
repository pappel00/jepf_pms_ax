package com.platform.module.form.service;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.file.service.FileStorageService;
import com.platform.module.form.domain.FormVersion;
import com.platform.module.form.dto.FormVersionDto;
import com.platform.module.form.exception.FormModuleErrorCode;
import com.platform.module.form.repository.FormGroupRepository;
import com.platform.module.form.repository.FormVersionRepository;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 표준양식 버전은 텍스트가 아니라 실제 엑셀/워드 파일로 관리한다(FormVersion 클래스 주석 참고).
 * 실제 디스크 저장/조회는 module-file의 FileStorageService를 그대로 재사용한다.
 */
@Service
@Transactional(readOnly = true)
public class FormVersionService {

    private final FormVersionRepository formVersionRepository;
    private final FormGroupRepository formGroupRepository;
    private final FileStorageService fileStorageService;

    public FormVersionService(FormVersionRepository formVersionRepository,
                               FormGroupRepository formGroupRepository,
                               FileStorageService fileStorageService) {
        this.formVersionRepository = formVersionRepository;
        this.formGroupRepository = formGroupRepository;
        this.fileStorageService = fileStorageService;
    }

    /** filePath를 직접 입력하는 메타데이터 전용 등록 (실제 파일 없이). */
    @Transactional
    public FormVersionDto.Response create(String groupId, FormVersionDto.CreateRequest request) {
        ensureGroupExists(groupId);
        FormVersion version = FormVersion.create(
                groupId, request.versionNo(), request.fileName(), request.filePath(), request.fileSize(),
                request.effectiveDate(), request.description());
        return FormVersionDto.Response.from(formVersionRepository.saveAndFlush(version));
    }

    /**
     * 실제 파일(엑셀/워드 등)을 업로드해서 디스크에 저장하고, 그 결과(경로/크기)로 FormVersion을
     * 만든다. 파일명/크기는 업로드된 파일에서 그대로 가져온다(사용자가 값을 조작해서 보낼 수
     * 있는 create()의 fileSize와 달리 신뢰할 수 있는 값이다).
     */
    @Transactional
    public FormVersionDto.Response upload(String groupId, MultipartFile file, String versionNo,
                                           LocalDate effectiveDate, String description) {
        ensureGroupExists(groupId);
        String subDir = TenantContext.getTenantId() + "/" + groupId;
        String relativePath = fileStorageService.save(file, subDir);

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "file";
        FormVersion version = FormVersion.create(
                groupId, versionNo, originalFilename, relativePath, file.getSize(), effectiveDate, description);
        return FormVersionDto.Response.from(formVersionRepository.saveAndFlush(version));
    }

    public List<FormVersionDto.Response> getList(String groupId) {
        return formVersionRepository.findAllByGroupIdOrderByEffectiveDateDesc(groupId).stream()
                .map(FormVersionDto.Response::from)
                .toList();
    }

    /** 다운로드용 — 실제 디스크 경로와 원본 파일명을 함께 돌려준다. */
    public DownloadTarget getDownloadTarget(String groupId, String id) {
        FormVersion version = findActiveOrThrow(groupId, id);
        Path diskPath = fileStorageService.resolve(version.getFilePath());
        if (!fileStorageService.exists(version.getFilePath())) {
            throw new TenantException(FormModuleErrorCode.FORM_FILE_NOT_FOUND_ON_DISK,
                    "메타데이터는 있지만 실제 파일을 디스크에서 찾을 수 없습니다: " + version.getFileName());
        }
        return new DownloadTarget(diskPath, version.getFileName());
    }

    /** 파일은 그대로 두고 메타데이터(버전번호/시행일자/비고/사용여부)만 수정한다. */
    @Transactional
    public FormVersionDto.Response update(String groupId, String id, FormVersionDto.UpdateRequest request) {
        FormVersion version = findActiveOrThrow(groupId, id);
        version.update(request.versionNo(), request.effectiveDate(), request.description());
        if ("Y".equals(request.useYn())) {
            version.activate();
        } else {
            version.deactivate();
        }
        return FormVersionDto.Response.from(version);
    }

    @Transactional
    public void delete(String groupId, String id) {
        findActiveOrThrow(groupId, id).markDeleted();
    }

    private FormVersion findActiveOrThrow(String groupId, String id) {
        return formVersionRepository.findByIdAndGroupId(id, groupId)
                .filter(version -> "N".equals(version.getDeletedYn()))
                .orElseThrow(() -> new TenantException(FormModuleErrorCode.FORM_VERSION_NOT_FOUND,
                        "존재하지 않는 양식 버전입니다: " + id));
    }

    private void ensureGroupExists(String groupId) {
        formGroupRepository.findByIdAndDeletedYn(groupId, "N")
                .orElseThrow(() -> new TenantException(FormModuleErrorCode.FORM_GROUP_NOT_FOUND,
                        "존재하지 않는 양식입니다: " + groupId));
    }

    public record DownloadTarget(Path diskPath, String fileName) {
    }
}
