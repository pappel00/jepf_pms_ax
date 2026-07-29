package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.MenuPage;
import com.platform.module.portal.dto.MenuPageDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.MenuPageRepository;
import com.platform.module.portal.repository.MenuRepository;
import com.platform.module.portal.repository.PageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MenuPage 매핑(N:M) 데이터 관리. 사진 속 "메뉴 관리" 화면에서 왼쪽 페이지를 체크해서
 * 오른쪽 선택된 폴더 하위로 저장하는 동작이 create()를 (여러 번, 체크한 개수만큼) 호출하는
 * 것이다 — 프론트에서 여러 건을 한 번에 보낼 별도 벌크 API는 만들지 않았다(다른 N:M
 * 매핑들도 전부 건별 API라 일관성을 맞췄고, 체크 개수가 아주 많지 않을 거라는 전제).
 */
@Service
@Transactional(readOnly = true)
public class MenuPageService {

    private final MenuPageRepository menuPageRepository;
    private final MenuRepository menuRepository;
    private final PageRepository pageRepository;

    public MenuPageService(MenuPageRepository menuPageRepository, MenuRepository menuRepository,
                            PageRepository pageRepository) {
        this.menuPageRepository = menuPageRepository;
        this.menuRepository = menuRepository;
        this.pageRepository = pageRepository;
    }

    @Transactional
    public MenuPageDto.Response create(String menuId, MenuPageDto.CreateRequest request) {
        ensureMenuExists(menuId);
        ensurePageExists(request.pageId());
        if (menuPageRepository.existsByMenuIdAndPageId(menuId, request.pageId())) {
            throw new BusinessException(PortalModuleErrorCode.MENU_PAGE_MAPPING_DUPLICATED,
                    "이미 이 폴더에 놓여있는 페이지입니다: " + request.pageId());
        }
        MenuPage mapping = MenuPage.create(menuId, request.pageId(), request.sortOrder());
        return MenuPageDto.Response.from(menuPageRepository.saveAndFlush(mapping));
    }

    public List<MenuPageDto.Response> getList(String menuId) {
        return menuPageRepository.findAllByMenuIdOrderBySortOrderAsc(menuId).stream()
                .map(MenuPageDto.Response::from)
                .toList();
    }

    @Transactional
    public void delete(String menuId, String mappingId) {
        MenuPage mapping = menuPageRepository.findByIdAndMenuId(mappingId, menuId)
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.MENU_PAGE_MAPPING_NOT_FOUND,
                        "존재하지 않는 매핑입니다: " + mappingId));
        menuPageRepository.delete(mapping);
    }

    private void ensureMenuExists(String menuId) {
        menuRepository.findByIdAndDeletedYn(menuId, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.MENU_NOT_FOUND,
                        "존재하지 않는 폴더입니다: " + menuId));
    }

    private void ensurePageExists(String pageId) {
        pageRepository.findByIdAndDeletedYn(pageId, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.PAGE_NOT_FOUND,
                        "존재하지 않는 페이지입니다: " + pageId));
    }
}
