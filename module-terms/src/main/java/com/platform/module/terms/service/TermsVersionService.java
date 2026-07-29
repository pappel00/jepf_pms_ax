package com.platform.module.terms.service;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.terms.domain.TermsVersion;
import com.platform.module.terms.dto.TermsVersionDto;
import com.platform.module.terms.exception.TermsModuleErrorCode;
import com.platform.module.terms.repository.TermsGroupRepository;
import com.platform.module.terms.repository.TermsVersionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TermsVersionService {

    private final TermsVersionRepository termsVersionRepository;
    private final TermsGroupRepository termsGroupRepository;

    public TermsVersionService(TermsVersionRepository termsVersionRepository, TermsGroupRepository termsGroupRepository) {
        this.termsVersionRepository = termsVersionRepository;
        this.termsGroupRepository = termsGroupRepository;
    }

    @Transactional
    public TermsVersionDto.Response create(String groupId, TermsVersionDto.CreateRequest request) {
        ensureGroupExists(groupId);
        TermsVersion termsVersion = TermsVersion.create(
                groupId, request.versionNo(), request.content(), request.effectiveDate(), request.description());
        return TermsVersionDto.Response.from(termsVersionRepository.saveAndFlush(termsVersion));
    }

    public List<TermsVersionDto.Response> getList(String groupId) {
        return termsVersionRepository.findAllByGroupIdOrderByEffectiveDateDesc(groupId).stream()
                .map(TermsVersionDto.Response::from)
                .toList();
    }

    @Transactional
    public TermsVersionDto.Response update(String groupId, String id, TermsVersionDto.UpdateRequest request) {
        TermsVersion termsVersion = findActiveOrThrow(groupId, id);
        termsVersion.update(request.versionNo(), request.content(), request.effectiveDate(), request.description());
        if ("Y".equals(request.useYn())) {
            termsVersion.activate();
        } else {
            termsVersion.deactivate();
        }
        return TermsVersionDto.Response.from(termsVersion);
    }

    @Transactional
    public void delete(String groupId, String id) {
        findActiveOrThrow(groupId, id).markDeleted();
    }

    private TermsVersion findActiveOrThrow(String groupId, String id) {
        return termsVersionRepository.findByIdAndGroupId(id, groupId)
                .filter(version -> "N".equals(version.getDeletedYn()))
                .orElseThrow(() -> new TenantException(TermsModuleErrorCode.TERMS_VERSION_NOT_FOUND,
                        "존재하지 않는 약관 버전입니다: " + id));
    }

    private void ensureGroupExists(String groupId) {
        termsGroupRepository.findByIdAndDeletedYn(groupId, "N")
                .orElseThrow(() -> new TenantException(TermsModuleErrorCode.TERMS_GROUP_NOT_FOUND,
                        "존재하지 않는 약관입니다: " + groupId));
    }
}
