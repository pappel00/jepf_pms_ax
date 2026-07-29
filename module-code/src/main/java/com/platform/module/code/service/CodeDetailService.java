package com.platform.module.code.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.code.domain.CodeDetail;
import com.platform.module.code.dto.CodeDetailDto;
import com.platform.module.code.exception.CodeModuleErrorCode;
import com.platform.module.code.repository.CodeDetailRepository;
import com.platform.module.code.repository.CodeGroupRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CodeDetailService {

    private final CodeDetailRepository codeDetailRepository;
    private final CodeGroupRepository codeGroupRepository;

    public CodeDetailService(CodeDetailRepository codeDetailRepository, CodeGroupRepository codeGroupRepository) {
        this.codeDetailRepository = codeDetailRepository;
        this.codeGroupRepository = codeGroupRepository;
    }

    @Transactional
    public CodeDetailDto.Response create(String groupId, CodeDetailDto.CreateRequest request) {
        ensureGroupExists(groupId);
        if (codeDetailRepository.existsByGroupIdAndCode(groupId, request.code())) {
            throw new BusinessException(CodeModuleErrorCode.CODE_DETAIL_CODE_DUPLICATED,
                    "이미 존재하는 코드입니다: " + request.code());
        }
        CodeDetail codeDetail = CodeDetail.create(
                groupId, request.code(), request.codeName(), request.sortOrder(), request.description());
        return CodeDetailDto.Response.from(codeDetailRepository.saveAndFlush(codeDetail));
    }

    public List<CodeDetailDto.Response> getList(String groupId) {
        return codeDetailRepository.findAllByGroupIdOrderBySortOrderAsc(groupId).stream()
                .map(CodeDetailDto.Response::from)
                .toList();
    }

    @Transactional
    public CodeDetailDto.Response update(String groupId, String id, CodeDetailDto.UpdateRequest request) {
        CodeDetail codeDetail = findActiveOrThrow(groupId, id);
        codeDetail.update(request.codeName(), request.sortOrder(), request.description());
        if ("Y".equals(request.useYn())) {
            codeDetail.activate();
        } else {
            codeDetail.deactivate();
        }
        return CodeDetailDto.Response.from(codeDetail);
    }

    @Transactional
    public void delete(String groupId, String id) {
        findActiveOrThrow(groupId, id).markDeleted();
    }

    private CodeDetail findActiveOrThrow(String groupId, String id) {
        return codeDetailRepository.findByIdAndGroupId(id, groupId)
                .filter(detail -> "N".equals(detail.getDeletedYn()))
                .orElseThrow(() -> new TenantException(CodeModuleErrorCode.CODE_DETAIL_NOT_FOUND,
                        "존재하지 않는 코드입니다: " + id));
    }

    private void ensureGroupExists(String groupId) {
        codeGroupRepository.findByIdAndDeletedYn(groupId, "N")
                .orElseThrow(() -> new TenantException(CodeModuleErrorCode.CODE_GROUP_NOT_FOUND,
                        "존재하지 않는 코드그룹입니다: " + groupId));
    }
}
