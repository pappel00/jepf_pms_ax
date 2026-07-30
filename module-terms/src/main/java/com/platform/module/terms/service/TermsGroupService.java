package com.platform.module.terms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.terms.domain.TermsGroup;
import com.platform.module.terms.dto.TermsGroupDto;
import com.platform.module.terms.exception.TermsModuleErrorCode;
import com.platform.module.terms.repository.TermsGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TermsGroupService {

    private final TermsGroupRepository termsGroupRepository;

    public TermsGroupService(TermsGroupRepository termsGroupRepository) {
        this.termsGroupRepository = termsGroupRepository;
    }

    @Transactional
    public TermsGroupDto.Response create(TermsGroupDto.CreateRequest request) {
        if (termsGroupRepository.existsByPortalIdAndTermsCode(request.portalId(), request.termsCode())) {
            throw new BusinessException(TermsModuleErrorCode.TERMS_GROUP_CODE_DUPLICATED,
                    "이 포탈에 이미 존재하는 약관입니다: " + request.termsCode());
        }
        TermsGroup termsGroup = TermsGroup.create(
                request.portalId(), request.termsCode(), request.termsName(), request.termsType(), request.description());
        return TermsGroupDto.Response.from(termsGroupRepository.saveAndFlush(termsGroup));
    }

    public TermsGroupDto.Response getById(String id) {
        return TermsGroupDto.Response.from(findActiveOrThrow(id));
    }

    /** portalId가 주어지면 그 포탈의 약관만, 없으면 테넌트 전체 약관을 조회한다. */
    public PagedResult<TermsGroupDto.Response> getList(String portalId, Pageable pageable) {
        Page<TermsGroup> page = (portalId != null && !portalId.isBlank())
                ? termsGroupRepository.findAllByPortalId(portalId, pageable)
                : termsGroupRepository.findAll(pageable);
        return new PagedResult<>(page.map(TermsGroupDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public TermsGroupDto.Response update(String id, TermsGroupDto.UpdateRequest request) {
        TermsGroup termsGroup = findActiveOrThrow(id);
        termsGroup.update(request.termsName(), request.termsType(), request.description());
        if ("Y".equals(request.useYn())) {
            termsGroup.activate();
        } else {
            termsGroup.deactivate();
        }
        return TermsGroupDto.Response.from(termsGroup);
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

    private TermsGroup findActiveOrThrow(String id) {
        return termsGroupRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(TermsModuleErrorCode.TERMS_GROUP_NOT_FOUND,
                        "존재하지 않는 약관입니다: " + id));
    }

    private TermsGroup findDeletedOrThrow(String id) {
        return termsGroupRepository.findByIdAndDeletedYn(id, "Y")
                .orElseThrow(() -> new TenantException(TermsModuleErrorCode.TERMS_GROUP_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 약관입니다: " + id));
    }
}
