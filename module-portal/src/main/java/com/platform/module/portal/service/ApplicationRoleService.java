package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.ApplicationRole;
import com.platform.module.portal.dto.ApplicationRoleDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.ApplicationRepository;
import com.platform.module.portal.repository.ApplicationRoleRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ApplicationRole 매핑 데이터 관리(CRUD) + 실제 인가 체크(isAccessible).
 * PageRoleService와 동일한 패턴이다 — 어플리케이션 1 : Role N.
 */
@Service
@Transactional(readOnly = true)
public class ApplicationRoleService {

    private final ApplicationRoleRepository applicationRoleRepository;
    private final ApplicationRepository applicationRepository;

    public ApplicationRoleService(ApplicationRoleRepository applicationRoleRepository,
                                   ApplicationRepository applicationRepository) {
        this.applicationRoleRepository = applicationRoleRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ApplicationRoleDto.Response create(String applicationId, ApplicationRoleDto.CreateRequest request) {
        ensureApplicationExists(applicationId);
        if (applicationRoleRepository.existsByApplicationIdAndRoleCode(applicationId, request.roleCode())) {
            throw new BusinessException(PortalModuleErrorCode.APPLICATION_ROLE_MAPPING_DUPLICATED,
                    "이미 매핑된 역할입니다: " + request.roleCode());
        }
        ApplicationRole mapping = ApplicationRole.create(applicationId, request.roleCode());
        return ApplicationRoleDto.Response.from(applicationRoleRepository.saveAndFlush(mapping));
    }

    public List<ApplicationRoleDto.Response> getList(String applicationId) {
        return applicationRoleRepository.findAllByApplicationId(applicationId).stream()
                .map(ApplicationRoleDto.Response::from)
                .toList();
    }

    @Transactional
    public void delete(String applicationId, String mappingId) {
        ApplicationRole mapping = applicationRoleRepository.findByIdAndApplicationId(mappingId, applicationId)
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.APPLICATION_ROLE_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + mappingId));
        applicationRoleRepository.delete(mapping);
    }

    /**
     * 이 어플리케이션에 매핑된 역할이 하나도 없으면 로그인한 사용자 누구나 접근 가능하다고 본다
     * (매핑을 깜빡한 새 어플리케이션이 기본적으로 전부 잠기는 것을 방지). 매핑이 하나라도 있으면,
     * 사용자가 가진 권한 중 하나라도 매핑된 roleCode 와 일치해야 접근 가능하다.
     */
    public boolean isAccessible(String applicationId, Collection<String> userAuthorities) {
        List<ApplicationRole> mappings = applicationRoleRepository.findAllByApplicationId(applicationId);
        if (mappings.isEmpty()) {
            return true;
        }
        return mappings.stream().anyMatch(mapping -> userAuthorities.contains(mapping.getRoleCode()));
    }

    private void ensureApplicationExists(String applicationId) {
        applicationRepository.findByIdAndDeletedYn(applicationId, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.APPLICATION_NOT_FOUND,
                        "존재하지 않는 어플리케이션입니다: " + applicationId));
    }
}
