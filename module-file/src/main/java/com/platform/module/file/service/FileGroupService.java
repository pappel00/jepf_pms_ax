package com.platform.module.file.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.file.domain.FileGroup;
import com.platform.module.file.dto.FileGroupDto;
import com.platform.module.file.exception.FileModuleErrorCode;
import com.platform.module.file.repository.FileGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FileGroupService {

    private final FileGroupRepository fileGroupRepository;

    public FileGroupService(FileGroupRepository fileGroupRepository) {
        this.fileGroupRepository = fileGroupRepository;
    }

    @Transactional
    public FileGroupDto.Response create(FileGroupDto.CreateRequest request) {
        if (fileGroupRepository.existsByGroupCode(request.groupCode())) {
            throw new BusinessException(FileModuleErrorCode.FILE_GROUP_CODE_DUPLICATED,
                    "이미 존재하는 첨부그룹입니다: " + request.groupCode());
        }
        FileGroup fileGroup = FileGroup.create(request.groupCode(), request.groupName(), request.description());
        return FileGroupDto.Response.from(fileGroupRepository.saveAndFlush(fileGroup));
    }

    public FileGroupDto.Response getById(String id) {
        return FileGroupDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<FileGroupDto.Response> getList(Pageable pageable) {
        Page<FileGroup> page = fileGroupRepository.findAll(pageable);
        return new PagedResult<>(page.map(FileGroupDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public FileGroupDto.Response update(String id, FileGroupDto.UpdateRequest request) {
        FileGroup fileGroup = findActiveOrThrow(id);
        fileGroup.update(request.groupName(), request.description());
        if ("Y".equals(request.useYn())) {
            fileGroup.activate();
        } else {
            fileGroup.deactivate();
        }
        return FileGroupDto.Response.from(fileGroup);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private FileGroup findActiveOrThrow(String id) {
        return fileGroupRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(FileModuleErrorCode.FILE_GROUP_NOT_FOUND,
                        "존재하지 않는 첨부그룹입니다: " + id));
    }
}
