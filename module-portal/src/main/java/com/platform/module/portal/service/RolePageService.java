package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.PageRole;
import com.platform.module.portal.dto.PageDto;
import com.platform.module.portal.dto.PageRoleDto;
import com.platform.module.portal.dto.RolePageDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.PageRepository;
import com.platform.module.portal.repository.PageRoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * roleCode 관점에서 "이 역할코드가 매핑된 페이지" 조회/추가/제거.
 * PageRoleMappingController(/api/v1/pages/{pageId}/roles, Page 관점)와 저장소를 공유하는
 * 반대 방향 API다.
 *
 * ⚠️ module-portal은 module-user(Role)에 의존하지 않으므로, Role의 PK가 아니라 roleCode
 * 문자열을 그대로 경로 파라미터로 받는다 — 그래서 /api/v1/roles/{roleId}/pages 가 아니라
 * /api/v1/pages/by-role/{roleCode} 로 매핑된다 (RolePageController 참고). roleCode가 실제
 * Role 마스터에 존재하는지도 여기서는 검증하지 않는다(PageRole 엔티티와 동일한 설계 이유).
 */
@Service
@Transactional(readOnly = true)
public class RolePageService {

    private final PageRoleRepository pageRoleRepository;
    private final PageRepository pageRepository;

    public RolePageService(PageRoleRepository pageRoleRepository, PageRepository pageRepository) {
        this.pageRoleRepository = pageRoleRepository;
        this.pageRepository = pageRepository;
    }

    @Transactional
    public PageRoleDto.Response addPage(String roleCode, RolePageDto.CreateRequest request) {
        pageRepository.findByIdAndDeletedYn(request.pageId(), "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_NOT_FOUND,
                        "존재하지 않는 페이지입니다: " + request.pageId()));
        if (pageRoleRepository.existsByPageIdAndRoleCode(request.pageId(), roleCode)) {
            throw new BusinessException(PortalModuleErrorCode.PAGE_ROLE_MAPPING_DUPLICATED,
                    "이미 매핑된 페이지입니다: " + request.pageId());
        }
        PageRole mapping = PageRole.create(request.pageId(), roleCode);
        return PageRoleDto.Response.from(pageRoleRepository.saveAndFlush(mapping));
    }

    public List<PageDto.Response> getPages(String roleCode) {
        List<String> pageIds = pageRoleRepository.findAllByRoleCode(roleCode).stream()
                .map(PageRole::getPageId)
                .toList();
        if (pageIds.isEmpty()) {
            return List.of();
        }
        return pageRepository.findAllById(pageIds).stream()
                .map(PageDto.Response::from)
                .toList();
    }

    @Transactional
    public void removePage(String roleCode, String mappingId) {
        PageRole mapping = pageRoleRepository.findById(mappingId)
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_ROLE_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + mappingId));
        pageRoleRepository.delete(mapping);
    }
}
