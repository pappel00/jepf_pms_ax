package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.portal.domain.Application;
import com.platform.module.portal.dto.ApplicationDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.ApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ApplicationDto.Response create(ApplicationDto.CreateRequest request) {
        if (applicationRepository.existsByApplicationCode(request.applicationCode())) {
            throw new BusinessException(PortalModuleErrorCode.APPLICATION_CODE_DUPLICATED,
                    "이미 존재하는 어플리케이션 코드입니다: " + request.applicationCode());
        }
        Application application = Application.create(
                request.applicationCode(), request.applicationName(), request.portalId());
        return ApplicationDto.Response.from(applicationRepository.saveAndFlush(application));
    }

    public ApplicationDto.Response getById(String id) {
        return ApplicationDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<ApplicationDto.Response> getList(Pageable pageable) {
        Page<Application> page = applicationRepository.findAll(pageable);
        return new PagedResult<>(page.map(ApplicationDto.Response::from).getContent(), PageInfo.of(page));
    }

    public PagedResult<ApplicationDto.Response> getListByPortal(String portalId, Pageable pageable) {
        Page<Application> page = applicationRepository.findAllByPortalId(portalId, pageable);
        return new PagedResult<>(page.map(ApplicationDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public ApplicationDto.Response update(String id, ApplicationDto.UpdateRequest request) {
        Application application = findActiveOrThrow(id);
        application.update(request.applicationName());
        if ("Y".equals(request.useYn())) {
            application.activate();
        } else {
            application.deactivate();
        }
        return ApplicationDto.Response.from(application);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private Application findActiveOrThrow(String id) {
        return applicationRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.APPLICATION_NOT_FOUND,
                        "존재하지 않는 어플리케이션입니다: " + id));
    }
}
