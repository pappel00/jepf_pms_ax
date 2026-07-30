package com.platform.module.privacy.service;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.privacy.domain.PrivacyPolicyVersion;
import com.platform.module.privacy.dto.PrivacyPolicyVersionDto;
import com.platform.module.privacy.exception.PrivacyModuleErrorCode;
import com.platform.module.privacy.repository.PrivacyPolicyGroupRepository;
import com.platform.module.privacy.repository.PrivacyPolicyVersionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrivacyPolicyVersionService {

    private final PrivacyPolicyVersionRepository privacyPolicyVersionRepository;
    private final PrivacyPolicyGroupRepository privacyPolicyGroupRepository;

    public PrivacyPolicyVersionService(PrivacyPolicyVersionRepository privacyPolicyVersionRepository,
                                        PrivacyPolicyGroupRepository privacyPolicyGroupRepository) {
        this.privacyPolicyVersionRepository = privacyPolicyVersionRepository;
        this.privacyPolicyGroupRepository = privacyPolicyGroupRepository;
    }

    @Transactional
    public PrivacyPolicyVersionDto.Response create(String groupId, PrivacyPolicyVersionDto.CreateRequest request) {
        ensureGroupExists(groupId);
        PrivacyPolicyVersion version = PrivacyPolicyVersion.create(
                groupId, request.versionNo(), request.content(), request.effectiveDate(), request.description());
        return PrivacyPolicyVersionDto.Response.from(privacyPolicyVersionRepository.saveAndFlush(version));
    }

    public List<PrivacyPolicyVersionDto.Response> getList(String groupId) {
        return privacyPolicyVersionRepository.findAllByGroupIdOrderByEffectiveDateDesc(groupId).stream()
                .map(PrivacyPolicyVersionDto.Response::from)
                .toList();
    }

    @Transactional
    public PrivacyPolicyVersionDto.Response update(String groupId, String id, PrivacyPolicyVersionDto.UpdateRequest request) {
        PrivacyPolicyVersion version = findActiveOrThrow(groupId, id);
        version.update(request.versionNo(), request.content(), request.effectiveDate(), request.description());
        if ("Y".equals(request.useYn())) {
            version.activate();
        } else {
            version.deactivate();
        }
        return PrivacyPolicyVersionDto.Response.from(version);
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

    private PrivacyPolicyVersion findActiveOrThrow(String groupId, String id) {
        return privacyPolicyVersionRepository.findByIdAndGroupId(id, groupId)
                .filter(version -> "N".equals(version.getDeletedYn()))
                .orElseThrow(() -> new TenantException(PrivacyModuleErrorCode.PRIVACY_VERSION_NOT_FOUND,
                        "존재하지 않는 방침 버전입니다: " + id));
    }

    private PrivacyPolicyVersion findDeletedOrThrow(String groupId, String id) {
        return privacyPolicyVersionRepository.findByIdAndGroupId(id, groupId)
                .filter(version -> "Y".equals(version.getDeletedYn()))
                .orElseThrow(() -> new TenantException(PrivacyModuleErrorCode.PRIVACY_VERSION_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 방침 버전입니다: " + id));
    }

    private void ensureGroupExists(String groupId) {
        privacyPolicyGroupRepository.findByIdAndDeletedYn(groupId, "N")
                .orElseThrow(() -> new TenantException(PrivacyModuleErrorCode.PRIVACY_GROUP_NOT_FOUND,
                        "존재하지 않는 방침입니다: " + groupId));
    }
}
