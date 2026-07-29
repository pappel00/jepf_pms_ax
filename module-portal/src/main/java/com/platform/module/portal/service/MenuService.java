package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.portal.domain.Menu;
import com.platform.module.portal.domain.MenuPage;
import com.platform.module.portal.dto.MenuDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.MenuPageRepository;
import com.platform.module.portal.repository.MenuRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class MenuService {

    private static final String ROOT_KEY = "__ROOT__";

    private final MenuRepository menuRepository;
    private final MenuPageRepository menuPageRepository;
    private final PageRoleService pageRoleService;

    public MenuService(MenuRepository menuRepository, MenuPageRepository menuPageRepository,
                        PageRoleService pageRoleService) {
        this.menuRepository = menuRepository;
        this.menuPageRepository = menuPageRepository;
        this.pageRoleService = pageRoleService;
    }

    @Transactional
    public MenuDto.Response create(MenuDto.CreateRequest request) {
        if (menuRepository.existsByMenuCode(request.menuCode())) {
            throw new BusinessException(PortalModuleErrorCode.MENU_CODE_DUPLICATED,
                    "이미 존재하는 메뉴 코드입니다: " + request.menuCode());
        }
        Menu menu = Menu.create(request.menuCode(), request.menuName(), request.applicationId(),
                request.parentMenuId(), request.sortOrder(), request.linkUrl());
        return MenuDto.Response.from(menuRepository.saveAndFlush(menu));
    }

    public MenuDto.Response getById(String id) {
        return MenuDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<MenuDto.Response> getList(Pageable pageable) {
        Page<Menu> page = menuRepository.findAll(pageable);
        return new PagedResult<>(page.map(MenuDto.Response::from).getContent(), PageInfo.of(page));
    }

    public PagedResult<MenuDto.Response> getListByApplication(String applicationId, Pageable pageable) {
        Page<Menu> page = menuRepository.findAllByApplicationId(applicationId, pageable);
        return new PagedResult<>(page.map(MenuDto.Response::from).getContent(), PageInfo.of(page));
    }

    /**
     * parentMenuId 기준으로 평면 리스트를 재귀적으로 조립해 계층 구조 그대로 내려준다.
     * 최상위(루트) 메뉴는 parentMenuId 가 null 인 것들이다. sortOrder 오름차순으로 정렬된 상태로 온다
     * (repository 쿼리 자체가 OrderBySortOrderAsc).
     *
     * MenuPage 매핑(N:M)과 연결되어 있다: 폴더에 매핑된 페이지 중 현재 로그인 사용자가 접근
     * 가능한 페이지의 id만 pageIds에 담긴다. 매핑된 페이지가 하나도 없거나(폴더 역할만 하는
     * 메뉴) 전부 접근 불가능해도, 폴더 자체는 트리에서 제외하지 않는다(예전엔 pageId가 있는
     * 메뉴 자체를 접근 불가 시 트리에서 뺐지만, N:M이 되면서 "폴더"와 "그 안의 페이지 접근권"이
     * 분리된 개념이 됐다 — 폴더 진입은 항상 되고, 안에서 못 보는 페이지만 안 보이는 게 자연스럽다).
     */
    public List<MenuDto.TreeNode> getTree(String applicationId) {
        List<Menu> menus = menuRepository.findAllByApplicationIdAndDeletedYnOrderBySortOrderAsc(applicationId, "N");

        Map<String, List<Menu>> childrenByParentId = menus.stream()
                .collect(Collectors.groupingBy(menu ->
                        StringUtils.hasText(menu.getParentMenuId()) ? menu.getParentMenuId() : ROOT_KEY));

        Set<String> userAuthorities = currentUserAuthorities();

        return buildChildren(ROOT_KEY, childrenByParentId, userAuthorities);
    }

    private List<MenuDto.TreeNode> buildChildren(String parentKey, Map<String, List<Menu>> childrenByParentId,
                                                  Set<String> userAuthorities) {
        return childrenByParentId.getOrDefault(parentKey, List.of()).stream()
                .map(menu -> MenuDto.TreeNode.of(menu,
                        accessiblePageIds(menu.getId(), userAuthorities),
                        buildChildren(menu.getId(), childrenByParentId, userAuthorities)))
                .toList();
    }

    private List<String> accessiblePageIds(String menuId, Set<String> userAuthorities) {
        return menuPageRepository.findAllByMenuIdOrderBySortOrderAsc(menuId).stream()
                .map(MenuPage::getPageId)
                .filter(pageId -> pageRoleService.isAccessible(pageId, userAuthorities))
                .toList();
    }

    private Set<String> currentUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    @Transactional
    public MenuDto.Response update(String id, MenuDto.UpdateRequest request) {
        Menu menu = findActiveOrThrow(id);
        menu.update(request.menuName(), request.sortOrder(), request.linkUrl());
        if ("Y".equals(request.useYn())) {
            menu.activate();
        } else {
            menu.deactivate();
        }
        return MenuDto.Response.from(menu);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private Menu findActiveOrThrow(String id) {
        return menuRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.MENU_NOT_FOUND,
                        "존재하지 않는 메뉴입니다: " + id));
    }
}
