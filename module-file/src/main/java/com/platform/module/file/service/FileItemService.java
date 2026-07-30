package com.platform.module.file.service;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.file.domain.FileItem;
import com.platform.module.file.dto.FileItemDto;
import com.platform.module.file.exception.FileModuleErrorCode;
import com.platform.module.file.repository.FileGroupRepository;
import com.platform.module.file.repository.FileItemRepository;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class FileItemService {

    private final FileItemRepository fileItemRepository;
    private final FileGroupRepository fileGroupRepository;
    private final FileStorageService fileStorageService;

    public FileItemService(FileItemRepository fileItemRepository, FileGroupRepository fileGroupRepository,
                            FileStorageService fileStorageService) {
        this.fileItemRepository = fileItemRepository;
        this.fileGroupRepository = fileGroupRepository;
        this.fileStorageService = fileStorageService;
    }

    /** 관리자가 filePath를 직접 입력해서 메타데이터만 등록하는 경로 (실제 파일 없이). */
    @Transactional
    public FileItemDto.Response create(String groupId, FileItemDto.CreateRequest request) {
        ensureGroupExists(groupId);
        FileItem fileItem = FileItem.create(
                groupId, request.fileName(), request.filePath(), request.fileSize(),
                request.sortOrder(), request.description());
        return FileItemDto.Response.from(fileItemRepository.saveAndFlush(fileItem));
    }

    /**
     * 실제 파일을 업로드해서 디스크에 저장하고, 그 결과(경로/크기)로 FileItem을 만든다.
     * 파일명은 업로드된 원본 파일명을 쓰고, fileSize는 실제 업로드된 바이트 수를 쓴다
     * (사용자가 값을 조작해서 보낼 수 있는 create()의 fileSize와 달리 신뢰할 수 있는 값이다).
     */
    @Transactional
    public FileItemDto.Response upload(String groupId, MultipartFile file, Integer sortOrder, String description) {
        ensureGroupExists(groupId);
        String subDir = TenantContext.getTenantId() + "/" + groupId;
        String relativePath = fileStorageService.save(file, subDir);

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "file";
        FileItem fileItem = FileItem.create(
                groupId, originalFilename, relativePath, file.getSize(),
                sortOrder != null ? sortOrder : 0, description);
        return FileItemDto.Response.from(fileItemRepository.saveAndFlush(fileItem));
    }

    public List<FileItemDto.Response> getList(String groupId) {
        return fileItemRepository.findAllByGroupIdOrderBySortOrderAsc(groupId).stream()
                .map(FileItemDto.Response::from)
                .toList();
    }

    /** 다운로드용 — 실제 디스크 경로와 원본 파일명을 함께 돌려준다. */
    public DownloadTarget getDownloadTarget(String groupId, String id) {
        FileItem fileItem = findActiveOrThrow(groupId, id);
        Path diskPath = fileStorageService.resolve(fileItem.getFilePath());
        if (!fileStorageService.exists(fileItem.getFilePath())) {
            throw new TenantException(FileModuleErrorCode.FILE_NOT_FOUND_ON_DISK,
                    "메타데이터는 있지만 실제 파일을 디스크에서 찾을 수 없습니다: " + fileItem.getFileName());
        }
        return new DownloadTarget(diskPath, fileItem.getFileName());
    }

    @Transactional
    public FileItemDto.Response update(String groupId, String id, FileItemDto.UpdateRequest request) {
        FileItem fileItem = findActiveOrThrow(groupId, id);
        fileItem.update(request.fileName(), request.filePath(), request.fileSize(),
                request.sortOrder(), request.description());
        if ("Y".equals(request.useYn())) {
            fileItem.activate();
        } else {
            fileItem.deactivate();
        }
        return FileItemDto.Response.from(fileItem);
    }

    @Transactional
    public void delete(String groupId, String id) {
        findActiveOrThrow(groupId, id).markDeleted();
    }

    /** 삭제된 행에서 "삭제"를 다시 누르면 404가 나던 문제의 대응 — 소프트 삭제를 되돌린다. */
    @Transactional
    public void restore(String groupId, String id) {
        findDeletedOrThrow(groupId, id).restore();
    }

    private FileItem findActiveOrThrow(String groupId, String id) {
        return fileItemRepository.findByIdAndGroupId(id, groupId)
                .filter(item -> "N".equals(item.getDeletedYn()))
                .orElseThrow(() -> new TenantException(FileModuleErrorCode.FILE_ITEM_NOT_FOUND,
                        "존재하지 않는 첨부항목입니다: " + id));
    }

    private FileItem findDeletedOrThrow(String groupId, String id) {
        return fileItemRepository.findByIdAndGroupId(id, groupId)
                .filter(item -> "Y".equals(item.getDeletedYn()))
                .orElseThrow(() -> new TenantException(FileModuleErrorCode.FILE_ITEM_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 첨부항목입니다: " + id));
    }

    private void ensureGroupExists(String groupId) {
        fileGroupRepository.findByIdAndDeletedYn(groupId, "N")
                .orElseThrow(() -> new TenantException(FileModuleErrorCode.FILE_GROUP_NOT_FOUND,
                        "존재하지 않는 첨부그룹입니다: " + groupId));
    }

    public record DownloadTarget(Path diskPath, String fileName) {
    }
}
