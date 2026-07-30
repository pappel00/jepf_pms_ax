package com.platform.module.code.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.code.domain.CodeGroup;
import com.platform.module.code.dto.CodeGroupDto;
import com.platform.module.code.exception.CodeModuleErrorCode;
import com.platform.module.code.repository.CodeGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CodeGroupService {

    private final CodeGroupRepository codeGroupRepository;

    public CodeGroupService(CodeGroupRepository codeGroupRepository) {
        this.codeGroupRepository = codeGroupRepository;
    }

    @Transactional
    public CodeGroupDto.Response create(CodeGroupDto.CreateRequest request) {
        if (codeGroupRepository.existsByGroupCode(request.groupCode())) {
            throw new BusinessException(CodeModuleErrorCode.CODE_GROUP_CODE_DUPLICATED,
                    "이미 존재하는 코드그룹입니다: " + request.groupCode());
        }
        CodeGroup codeGroup = CodeGroup.create(request.groupCode(), request.groupName(), request.description());
        return CodeGroupDto.Response.from(codeGroupRepository.saveAndFlush(codeGroup));
    }

    public CodeGroupDto.Response getById(String id) {
        return CodeGroupDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<CodeGroupDto.Response> getList(Pageable pageable) {
        Page<CodeGroup> page = codeGroupRepository.findAll(pageable);
        return new PagedResult<>(page.map(CodeGroupDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public CodeGroupDto.Response update(String id, CodeGroupDto.UpdateRequest request) {
        CodeGroup codeGroup = findActiveOrThrow(id);
        codeGroup.update(request.groupName(), request.description());
        if ("Y".equals(request.useYn())) {
            codeGroup.activate();
        } else {
            codeGroup.deactivate();
        }
        return CodeGroupDto.Response.from(codeGroup);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    /** 삭제된 행에서 "삭제"를 다시 누르면 404가 나던 문제의 대응 — 소프트 삭제를 되돌린다. */
    @Transactional
    public void restore(String id) {
        findDeletedOrThrow(id).restore();
    }

    private CodeGroup findActiveOrThrow(String id) {
        return codeGroupRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(CodeModuleErrorCode.CODE_GROUP_NOT_FOUND,
                        "존재하지 않는 코드그룹입니다: " + id));
    }

    private CodeGroup findDeletedOrThrow(String id) {
        return codeGroupRepository.findByIdAndDeletedYn(id, "Y")
                .orElseThrow(() -> new TenantException(CodeModuleErrorCode.CODE_GROUP_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 코드그룹입니다: " + id));
    }
}
