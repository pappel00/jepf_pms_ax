package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.portal.domain.Application;
import com.platform.module.portal.dto.ApplicationDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.ApplicationRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationRoleService applicationRoleService;

    public ApplicationService(ApplicationRepository applicationRepository, ApplicationRoleService applicationRoleService) {
        this.applicationRepository = applicationRepository;
        this.applicationRoleService = applicationRoleService;
    }

    @Transactional
    public ApplicationDto.Response create(ApplicationDto.CreateRequest request) {
        if (applicationRepository.existsByApplicationCode(request.applicationCode())) {
            throw new BusinessException(PortalModuleErrorCode.APPLICATION_CODE_DUPLICATED,
                    "이미 존재하는 어플리케이션 코드입니다: " + request.applicationCode());
        }
        Application application = Application.create(
                request.applicationCode(), request.applicationName(), request.portalId(), request.sortOrder());
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

    /**
     * 운영 화면 셸(어플리케이션 선택 UI)용 — 현재 로그인한 사용자가 ApplicationRole 기준으로
     * 접근 가능한 어플리케이션만, sortOrder 순으로, 사용 중(useYn=Y)인 것만 내려준다.
     * 관리자 목록(getList/getListByPortal)과 달리 비활성/접근불가 항목은 아예 제외한다 —
     * 실제 사용자가 보는 내비게이션이라 관리 화면과 성격이 다르다.
     */
    public List<ApplicationDto.Response> getAccessibleList(String portalId, Collection<String> userAuthorities) {
        List<Application> applications = portalId != null
                ? applicationRepository.findAllByPortalIdAndDeletedYnOrderBySortOrderAsc(portalId, "N")
                : applicationRepository.findAllByDeletedYnOrderBySortOrderAsc("N");
        return applications.stream()
                .filter(app -> "Y".equals(app.getUseYn()))
                .filter(app -> applicationRoleService.isAccessible(app.getId(), userAuthorities))
                .map(ApplicationDto.Response::from)
                .toList();
    }

    @Transactional
    public ApplicationDto.Response update(String id, ApplicationDto.UpdateRequest request) {
        Application application = findActiveOrThrow(id);
        application.update(request.applicationName(), request.sortOrder());
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

    /** 삭제된 행에서 "삭제"를 다시 누르면 404가 나던 문제의 대응 — 소프트 삭제를 되돌린다. */
    @Transactional
    public void restore(String id) {
        findDeletedOrThrow(id).restore();
    }

    private Application findActiveOrThrow(String id) {
        return applicationRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.APPLICATION_NOT_FOUND,
                        "존재하지 않는 어플리케이션입니다: " + id));
    }

    private Application findDeletedOrThrow(String id) {
        return applicationRepository.findByIdAndDeletedYn(id, "Y")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.APPLICATION_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 어플리케이션입니다: " + id));
    }
}
