package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.PageRole;
import com.platform.module.portal.dto.PageRoleDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.PageRepository;
import com.platform.module.portal.repository.PageRoleRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PageRole 매핑 데이터 관리(CRUD) + 실제 인가 체크(isAccessible).
 *
 * roleCode 는 Role 엔티티의 PK가 아니라 Spring Security GrantedAuthority 문자열과 정확히
 * 일치해야 하는 값이므로(PageRole 엔티티 주석 참고), module-user 를 몰라도 이 서비스만으로
 * 실제 인가 체크가 가능하다 — PageController.checkAccess(), MenuService.getTree() 가 이 메서드를 쓴다.
 */
@Service
@Transactional(readOnly = true)
public class PageRoleService {

    private final PageRoleRepository pageRoleRepository;
    private final PageRepository pageRepository;

    public PageRoleService(PageRoleRepository pageRoleRepository, PageRepository pageRepository) {
        this.pageRoleRepository = pageRoleRepository;
        this.pageRepository = pageRepository;
    }

    @Transactional
    public PageRoleDto.Response create(String pageId, PageRoleDto.CreateRequest request) {
        ensurePageExists(pageId);
        if (pageRoleRepository.existsByPageIdAndRoleCode(pageId, request.roleCode())) {
            throw new BusinessException(PortalModuleErrorCode.PAGE_ROLE_MAPPING_DUPLICATED,
                    "이미 매핑된 역할입니다: " + request.roleCode());
        }
        PageRole mapping = PageRole.create(pageId, request.roleCode());
        return PageRoleDto.Response.from(pageRoleRepository.saveAndFlush(mapping));
    }

    public List<PageRoleDto.Response> getList(String pageId) {
        return pageRoleRepository.findAllByPageId(pageId).stream()
                .map(PageRoleDto.Response::from)
                .toList();
    }

    @Transactional
    public void delete(String pageId, String mappingId) {
        PageRole mapping = pageRoleRepository.findByIdAndPageId(mappingId, pageId)
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_ROLE_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + mappingId));
        pageRoleRepository.delete(mapping);
    }

    /**
     * 이 페이지에 매핑된 역할이 하나도 없으면 로그인한 사용자 누구나 접근 가능하다고 본다
     * (매핑을 깜빡한 새 페이지가 기본적으로 전부 잠기는 것을 방지). 매핑이 하나라도 있으면,
     * 사용자가 가진 권한 중 하나라도 매핑된 roleCode 와 일치해야 접근 가능하다.
     */
    public boolean isAccessible(String pageId, Collection<String> userAuthorities) {
        List<PageRole> mappings = pageRoleRepository.findAllByPageId(pageId);
        if (mappings.isEmpty()) {
            return true;
        }
        return mappings.stream().anyMatch(mapping -> userAuthorities.contains(mapping.getRoleCode()));
    }

    private void ensurePageExists(String pageId) {
        pageRepository.findByIdAndDeletedYn(pageId, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_NOT_FOUND,
                        "존재하지 않는 페이지입니다: " + pageId));
    }
}
