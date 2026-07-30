package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.portal.domain.Page;
import com.platform.module.portal.dto.PageDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.PageRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PageService {

    private final PageRepository pageRepository;

    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Transactional
    public PageDto.Response create(PageDto.CreateRequest request) {
        if (pageRepository.existsByPageCode(request.pageCode())) {
            throw new BusinessException(PortalModuleErrorCode.PAGE_CODE_DUPLICATED,
                    "이미 존재하는 페이지 코드입니다: " + request.pageCode());
        }
        Page page = Page.create(request.pageCode(), request.pageName(), request.pageType(),
                request.deviceType(), request.pageFile(), request.messageResourceCode());
        return PageDto.Response.from(pageRepository.saveAndFlush(page));
    }

    public PageDto.Response getById(String id) {
        return PageDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<PageDto.Response> getList(Pageable pageable) {
        org.springframework.data.domain.Page<Page> page = pageRepository.findAll(pageable);
        return new PagedResult<>(page.map(PageDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public PageDto.Response update(String id, PageDto.UpdateRequest request) {
        Page page = findActiveOrThrow(id);
        page.update(request.pageName(), request.pageType(), request.deviceType(),
                request.pageFile(), request.messageResourceCode());
        if ("Y".equals(request.useYn())) {
            page.activate();
        } else {
            page.deactivate();
        }
        return PageDto.Response.from(page);
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

    private Page findActiveOrThrow(String id) {
        return pageRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_NOT_FOUND,
                        "존재하지 않는 페이지입니다: " + id));
    }

    private Page findDeletedOrThrow(String id) {
        return pageRepository.findByIdAndDeletedYn(id, "Y")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 페이지입니다: " + id));
    }
}
