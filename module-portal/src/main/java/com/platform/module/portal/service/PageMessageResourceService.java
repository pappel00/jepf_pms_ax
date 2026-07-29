package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.PageMessageResource;
import com.platform.module.portal.dto.PageMessageResourceDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.PageMessageResourceRepository;
import com.platform.module.portal.repository.PageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PageMessageResourceService {

    private final PageMessageResourceRepository pageMessageResourceRepository;
    private final PageRepository pageRepository;

    public PageMessageResourceService(PageMessageResourceRepository pageMessageResourceRepository,
                                       PageRepository pageRepository) {
        this.pageMessageResourceRepository = pageMessageResourceRepository;
        this.pageRepository = pageRepository;
    }

    @Transactional
    public PageMessageResourceDto.Response create(String pageId, PageMessageResourceDto.CreateRequest request) {
        ensurePageExists(pageId);
        if (pageMessageResourceRepository.existsByPageIdAndMessageResourceCode(pageId, request.messageResourceCode())) {
            throw new BusinessException(PortalModuleErrorCode.PAGE_MESSAGE_RESOURCE_MAPPING_DUPLICATED,
                    "이미 매핑된 메시지자원입니다: " + request.messageResourceCode());
        }
        PageMessageResource mapping = PageMessageResource.create(pageId, request.messageResourceCode());
        return PageMessageResourceDto.Response.from(pageMessageResourceRepository.saveAndFlush(mapping));
    }

    public List<PageMessageResourceDto.Response> getList(String pageId) {
        return pageMessageResourceRepository.findAllByPageId(pageId).stream()
                .map(PageMessageResourceDto.Response::from)
                .toList();
    }

    @Transactional
    public void delete(String pageId, String mappingId) {
        PageMessageResource mapping = pageMessageResourceRepository.findByIdAndPageId(mappingId, pageId)
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_MESSAGE_RESOURCE_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + mappingId));
        pageMessageResourceRepository.delete(mapping);
    }

    private void ensurePageExists(String pageId) {
        pageRepository.findByIdAndDeletedYn(pageId, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_NOT_FOUND,
                        "존재하지 않는 페이지입니다: " + pageId));
    }
}
