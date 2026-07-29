package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Menu(폴더) - Page 매핑(N:M). 사진 속 "메뉴 관리" 화면에서 왼쪽의 페이지를 체크해서
 * 오른쪽에 선택된 폴더 하위로 저장하는 동작이 이 매핑을 만드는 것이다.
 *
 * 하나의 폴더 아래에 여러 페이지를 놓을 수 있고, 같은 페이지를 여러 폴더에 동시에 놓을 수도
 * 있다(예: "자주찾는 메뉴" 폴더 + 원래 소속 폴더 둘 다) — 그래서 1:N(Menu.pageId)이 아니라
 * N:M 매핑 테이블로 관리한다. Page의 다른 N:M 매핑들(PageRole 등)과 동일한 패턴(하드 삭제)이다.
 *
 * sortOrder 로 같은 폴더 안에서 페이지들의 노출 순서를 정한다.
 */
@Entity
@Table(name = "prt_menu_page",
        uniqueConstraints = @UniqueConstraint(name = "uk_mp_tenant_menu_page",
                columnNames = {"tenant_id", "menu_id", "page_id"}))
public class MenuPage extends TenantAwareEntity {

    @Column(name = "menu_id", length = 64, nullable = false)
    private String menuId;

    @Column(name = "page_id", length = 64, nullable = false)
    private String pageId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected MenuPage() {
    }

    private MenuPage(String menuId, String pageId, int sortOrder) {
        super(PrefixedIdGenerator.generate("MNP"));
        this.menuId = menuId;
        this.pageId = pageId;
        this.sortOrder = sortOrder;
    }

    public static MenuPage create(String menuId, String pageId, int sortOrder) {
        return new MenuPage(menuId, pageId, sortOrder);
    }

    public String getMenuId() {
        return menuId;
    }

    public String getPageId() {
        return pageId;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
