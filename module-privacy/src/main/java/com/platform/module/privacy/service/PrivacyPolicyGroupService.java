package com.platform.module.privacy.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.privacy.domain.PrivacyPolicyGroup;
import com.platform.module.privacy.dto.PrivacyPolicyGroupDto;
import com.platform.module.privacy.exception.PrivacyModuleErrorCode;
import com.platform.module.privacy.repository.PrivacyPolicyGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrivacyPolicyGroupService {

    private final PrivacyPolicyGroupRepository privacyPolicyGroupRepository;

    public PrivacyPolicyGroupService(PrivacyPolicyGroupRepository privacyPolicyGroupRepository) {
        this.privacyPolicyGroupRepository = privacyPolicyGroupRepository;
    }

    @Transactional
    public PrivacyPolicyGroupDto.Response create(PrivacyPolicyGroupDto.CreateRequest request) {
        if (privacyPolicyGroupRepository.existsByPortalIdAndPolicyCode(request.portalId(), request.policyCode())) {
            throw new BusinessException(PrivacyModuleErrorCode.PRIVACY_GROUP_CODE_DUPLICATED,
                    "이 포탈에 이미 존재하는 방침입니다: " + request.policyCode());
        }
        PrivacyPolicyGroup group = PrivacyPolicyGroup.create(
                request.portalId(), request.policyCode(), request.policyName(), request.description());
        return PrivacyPolicyGroupDto.Response.from(privacyPolicyGroupRepository.saveAndFlush(group));
    }

    public PrivacyPolicyGroupDto.Response getById(String id) {
        return PrivacyPolicyGroupDto.Response.from(findActiveOrThrow(id));
    }

    /** portalId가 주어지면 그 포탈의 방침만, 없으면 테넌트 전체를 조회한다. */
    public PagedResult<PrivacyPolicyGroupDto.Response> getList(String portalId, Pageable pageable) {
        Page<PrivacyPolicyGroup> page = (portalId != null && !portalId.isBlank())
                ? privacyPolicyGroupRepository.findAllByPortalId(portalId, pageable)
                : privacyPolicyGroupRepository.findAll(pageable);
        return new PagedResult<>(page.map(PrivacyPolicyGroupDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public PrivacyPolicyGroupDto.Response update(String id, PrivacyPolicyGroupDto.UpdateRequest request) {
        PrivacyPolicyGroup group = findActiveOrThrow(id);
        group.update(request.policyName(), request.description());
        if ("Y".equals(request.useYn())) {
            group.activate();
        } else {
            group.deactivate();
        }
        return PrivacyPolicyGroupDto.Response.from(group);
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

    private PrivacyPolicyGroup findActiveOrThrow(String id) {
        return privacyPolicyGroupRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PrivacyModuleErrorCode.PRIVACY_GROUP_NOT_FOUND,
                        "존재하지 않는 방침입니다: " + id));
    }

    private PrivacyPolicyGroup findDeletedOrThrow(String id) {
        return privacyPolicyGroupRepository.findByIdAndDeletedYn(id, "Y")
                .orElseThrow(() -> new TenantException(PrivacyModuleErrorCode.PRIVACY_GROUP_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 방침입니다: " + id));
    }
}
