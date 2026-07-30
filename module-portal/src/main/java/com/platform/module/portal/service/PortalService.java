package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.portal.domain.Portal;
import com.platform.module.portal.dto.PortalDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.PortalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PortalService {

    private final PortalRepository portalRepository;

    public PortalService(PortalRepository portalRepository) {
        this.portalRepository = portalRepository;
    }

    @Transactional
    public PortalDto.Response create(PortalDto.CreateRequest request) {
        if (portalRepository.existsByPortalCode(request.portalCode())) {
            throw new BusinessException(PortalModuleErrorCode.PORTAL_CODE_DUPLICATED,
                    "이미 존재하는 포탈 코드입니다: " + request.portalCode());
        }
        Portal portal = Portal.create(request.portalCode(), request.portalName(),
                request.deviceType(), request.description());
        return PortalDto.Response.from(portalRepository.saveAndFlush(portal));
    }

    public PortalDto.Response getById(String id) {
        return PortalDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<PortalDto.Response> getList(Pageable pageable) {
        Page<Portal> page = portalRepository.findAll(pageable);
        return new PagedResult<>(page.map(PortalDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public PortalDto.Response update(String id, PortalDto.UpdateRequest request) {
        Portal portal = findActiveOrThrow(id);
        portal.update(request.portalName(), request.deviceType(), request.description());
        if ("Y".equals(request.useYn())) {
            portal.activate();
        } else {
            portal.deactivate();
        }
        return PortalDto.Response.from(portal);
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

    private Portal findActiveOrThrow(String id) {
        return portalRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PORTAL_NOT_FOUND,
                        "존재하지 않는 포탈입니다: " + id));
    }

    private Portal findDeletedOrThrow(String id) {
        return portalRepository.findByIdAndDeletedYn(id, "Y")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PORTAL_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 포탈입니다: " + id));
    }
}
