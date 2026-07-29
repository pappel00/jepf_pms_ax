package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.PageDataService;
import com.platform.module.portal.dto.PageDataServiceDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.PageDataServiceRepository;
import com.platform.module.portal.repository.PageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PageDataServiceService {

    private final PageDataServiceRepository pageDataServiceRepository;
    private final PageRepository pageRepository;

    public PageDataServiceService(PageDataServiceRepository pageDataServiceRepository, PageRepository pageRepository) {
        this.pageDataServiceRepository = pageDataServiceRepository;
        this.pageRepository = pageRepository;
    }

    @Transactional
    public PageDataServiceDto.Response create(String pageId, PageDataServiceDto.CreateRequest request) {
        ensurePageExists(pageId);
        if (pageDataServiceRepository.existsByPageIdAndDataServiceId(pageId, request.dataServiceId())) {
            throw new BusinessException(PortalModuleErrorCode.PAGE_DATA_SERVICE_MAPPING_DUPLICATED,
                    "이미 매핑된 데이터서비스입니다: " + request.dataServiceId());
        }
        PageDataService mapping = PageDataService.create(pageId, request.dataServiceId());
        return PageDataServiceDto.Response.from(pageDataServiceRepository.saveAndFlush(mapping));
    }

    public List<PageDataServiceDto.Response> getList(String pageId) {
        return pageDataServiceRepository.findAllByPageId(pageId).stream()
                .map(PageDataServiceDto.Response::from)
                .toList();
    }

    @Transactional
    public void delete(String pageId, String mappingId) {
        PageDataService mapping = pageDataServiceRepository.findByIdAndPageId(mappingId, pageId)
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_DATA_SERVICE_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + mappingId));
        pageDataServiceRepository.delete(mapping);
    }

    private void ensurePageExists(String pageId) {
        pageRepository.findByIdAndDeletedYn(pageId, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_NOT_FOUND,
                        "존재하지 않는 페이지입니다: " + pageId));
    }
}
